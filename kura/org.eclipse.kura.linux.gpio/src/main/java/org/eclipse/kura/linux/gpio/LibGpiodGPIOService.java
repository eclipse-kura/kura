package org.eclipse.kura.linux.gpio;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;

/**
 * Implementation of GPIOService using libgpiod v2.2.x
 *
 * This service manages GPIO pins across multiple GPIO chips and provides
 * pin discovery, name resolution, and pin lifecycle management.
 */
public class LibGpiodGPIOService implements GPIOService {

    private static final Logger logger = LoggerFactory.getLogger(LibGpiodGPIOService.class);
    private static final String GPIO_CHIP_PREFIX = "/dev/gpiochip";
    private static final Pattern GPIO_CHIP_PATTERN = Pattern.compile("^gpiochip\\d+$");

    // Default chip path if no specific chip is found
    private static final String DEFAULT_CHIP_PATH = "/dev/gpiochip0";

    // Cache for available pins across all chips
    private final Map<Integer, String> availablePins = new ConcurrentHashMap<>();
    private final Map<String, Integer> pinNameToOffset = new ConcurrentHashMap<>();
    private final Map<String, String> pinNameToChip = new ConcurrentHashMap<>();

    // Cache for created pins to avoid duplicates
    private final Map<String, KuraGPIOPin> pinCache = new ConcurrentHashMap<>();

    // Default configurations
    private final KuraGPIODirection defaultDirection = KuraGPIODirection.INPUT;
    private final KuraGPIOMode defaultMode = KuraGPIOMode.INPUT_PULL_UP;
    private final KuraGPIOTrigger defaultTrigger = KuraGPIOTrigger.NONE;

    // Initialization flag
    private volatile boolean initialized = false;

    protected void activate() {
        logger.info("activating libgpiod GPIOService...");
        initialize();
        logger.info("activating libgpiod GPIOService...Done!");
    }

    protected void deactivate() {
        logger.info("deactivating libgpiod GPIOService");
    }

    /**
     * Initialize the GPIO service by discovering available GPIO chips and pins
     */
    public void initialize() {
        if (this.initialized) {
            return;
        }

        synchronized (this) {
            if (this.initialized) {
                return;
            }

            discoverGPIOChips();
            this.initialized = true;
        }
    }

    /**
     * Discover all available GPIO chips and their pins
     */
    private void discoverGPIOChips() {
        this.availablePins.clear();
        this.pinNameToOffset.clear();
        this.pinNameToChip.clear();

        // Find all GPIO chip devices
        File devDir = new File("/dev");
        if (!devDir.exists() || !devDir.isDirectory()) {
            return;
        }

        File[] chipFiles = devDir.listFiles((dir, name) -> GPIO_CHIP_PATTERN.matcher(name).matches());
        if (chipFiles == null || chipFiles.length == 0) {
            // If no chips found, assume default chip exists
            discoverChipPins(DEFAULT_CHIP_PATH);
            return;
        }

        // Sort chip files by number for consistent ordering
        Arrays.sort(chipFiles, (a, b) -> {
            int numA = extractChipNumber(a.getName());
            int numB = extractChipNumber(b.getName());
            return Integer.compare(numA, numB);
        });

        // Discover pins for each chip
        for (File chipFile : chipFiles) {
            discoverChipPins(chipFile.getAbsolutePath());
        }
    }

    /**
     * Discover pins for a specific GPIO chip
     */
    private void discoverChipPins(String chipPath) {
        Pointer chip = null;
        Pointer chipInfo = null;

        try {
            // Open the chip
            chip = LibGpiodNative.INSTANCE.gpiod_chip_open(chipPath);
            if (chip == null) {
                return;
            }

            // Get chip info
            chipInfo = LibGpiodNative.INSTANCE.gpiod_chip_get_info(chip);
            if (chipInfo == null) {
                return;
            }

            String chipName = LibGpiodNative.INSTANCE.gpiod_chip_info_get_name(chipInfo);
            long numLines = LibGpiodNative.INSTANCE.gpiod_chip_info_get_num_lines(chipInfo);

            // Discover individual pins
            for (int offset = 0; offset < numLines; offset++) {
                Pointer lineInfo = null;
                try {
                    lineInfo = LibGpiodNative.INSTANCE.gpiod_chip_get_line_info(chip, offset);
                    if (lineInfo != null) {
                        String lineName = LibGpiodNative.INSTANCE.gpiod_line_info_get_name(lineInfo);

                        // Use line name if available, otherwise create default name
                        String pinName = lineName != null && !lineName.trim().isEmpty() ? lineName.trim()
                                : chipName + "_GPIO_" + offset;

                        // Calculate global pin number (chip_number * 1000 + offset)
                        int chipNumber = extractChipNumber(new File(chipPath).getName());
                        int globalPin = chipNumber * 1000 + offset;

                        this.availablePins.put(globalPin, pinName);
                        this.pinNameToOffset.put(pinName, offset);
                        this.pinNameToChip.put(pinName, chipPath);

                        // Also add by offset as alternative name
                        String offsetName = "GPIO_" + offset;
                        if (!this.pinNameToOffset.containsKey(offsetName)) {
                            this.pinNameToOffset.put(offsetName, offset);
                            this.pinNameToChip.put(offsetName, chipPath);
                        }
                    }
                } finally {
                    if (lineInfo != null) {
                        LibGpiodNative.INSTANCE.gpiod_line_info_free(lineInfo);
                    }
                }
            }

        } finally {
            if (chipInfo != null) {
                LibGpiodNative.INSTANCE.gpiod_chip_info_free(chipInfo);
            }
            if (chip != null) {
                LibGpiodNative.INSTANCE.gpiod_chip_close(chip);
            }
        }
    }

    /**
     * Extract chip number from chip device name (e.g., "gpiochip0" -> 0)
     */
    private int extractChipNumber(String chipName) {
        try {
            return Integer.parseInt(chipName.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public KuraGPIOPin getPinByName(String pinName) {
        return getPinByName(pinName, this.defaultDirection, this.defaultMode, this.defaultTrigger);
    }

    @Override
    public KuraGPIOPin getPinByName(String pinName, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        if (pinName == null || pinName.trim().isEmpty()) {
            throw new IllegalArgumentException("Pin name cannot be null or empty");
        }

        initialize();

        // Check cache first
        String cacheKey = createCacheKey(pinName, direction, mode, trigger);
        KuraGPIOPin cachedPin = this.pinCache.get(cacheKey);
        if (cachedPin != null) {
            return cachedPin;
        }

        // Look up pin by name
        Integer offset = this.pinNameToOffset.get(pinName);
        String chipPath = this.pinNameToChip.get(pinName);

        if (offset == null || chipPath == null) {
            throw new IllegalArgumentException("Pin not found: " + pinName);
        }

        // Create the pin
        KuraGPIOPin pin = LibGpiodPinFactory.createPin(chipPath, offset, direction, mode, trigger, pinName);

        // Cache the pin
        this.pinCache.put(cacheKey, pin);

        return pin;
    }

    @Override
    public KuraGPIOPin getPinByTerminal(int terminal) {
        return getPinByTerminal(terminal, this.defaultDirection, this.defaultMode, this.defaultTrigger);
    }

    @Override
    public KuraGPIOPin getPinByTerminal(int terminal, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        if (terminal < 0) {
            throw new IllegalArgumentException("Terminal number must be non-negative");
        }

        initialize();

        // Check cache first
        String cacheKey = createCacheKey("TERMINAL_" + terminal, direction, mode, trigger);
        KuraGPIOPin cachedPin = this.pinCache.get(cacheKey);
        if (cachedPin != null) {
            return cachedPin;
        }

        // For terminal-based access, we need to map the terminal to chip and offset
        String chipPath = DEFAULT_CHIP_PATH;
        int offset = terminal;

        // Check if this is a global pin number (chip_number * 1000 + offset)
        if (terminal >= 1000) {
            int chipNumber = terminal / 1000;
            offset = terminal % 1000;
            chipPath = "/dev/gpiochip" + chipNumber;
        }

        // Verify the pin exists
        if (!isValidPin(chipPath, offset)) {
            throw new IllegalArgumentException("Invalid terminal: " + terminal);
        }

        String pinName = "GPIO_" + offset;

        // Create the pin
        KuraGPIOPin pin = LibGpiodPinFactory.createPin(chipPath, offset, direction, mode, trigger, pinName);

        // Cache the pin
        this.pinCache.put(cacheKey, pin);

        return pin;
    }

    /**
     * Verify if a pin exists on the specified chip
     */
    private boolean isValidPin(String chipPath, int offset) {
        Pointer chip = null;
        Pointer chipInfo = null;

        try {
            chip = LibGpiodNative.INSTANCE.gpiod_chip_open(chipPath);
            if (chip == null) {
                return false;
            }

            chipInfo = LibGpiodNative.INSTANCE.gpiod_chip_get_info(chip);
            if (chipInfo == null) {
                return false;
            }

            long numLines = LibGpiodNative.INSTANCE.gpiod_chip_info_get_num_lines(chipInfo);
            return offset >= 0 && offset < numLines;

        } finally {
            if (chipInfo != null) {
                LibGpiodNative.INSTANCE.gpiod_chip_info_free(chipInfo);
            }
            if (chip != null) {
                LibGpiodNative.INSTANCE.gpiod_chip_close(chip);
            }
        }
    }

    @Override
    public Map<Integer, String> getAvailablePins() {
        initialize();
        return new HashMap<>(this.availablePins);
    }

    /**
     * Get available pins for a specific chip
     */
    public Map<Integer, String> getAvailablePins(String chipPath) {
        Map<Integer, String> chipPins = new HashMap<>();

        for (Map.Entry<String, String> entry : this.pinNameToChip.entrySet()) {
            if (chipPath.equals(entry.getValue())) {
                String pinName = entry.getKey();
                Integer offset = this.pinNameToOffset.get(pinName);
                if (offset != null) {
                    chipPins.put(offset, pinName);
                }
            }
        }

        return chipPins;
    }

    /**
     * Get list of available GPIO chips
     */
    public List<String> getAvailableChips() {
        Set<String> chips = new HashSet<>();

        File devDir = new File("/dev");
        if (devDir.exists() && devDir.isDirectory()) {
            File[] chipFiles = devDir.listFiles((dir, name) -> GPIO_CHIP_PATTERN.matcher(name).matches());
            if (chipFiles != null) {
                for (File chipFile : chipFiles) {
                    chips.add(chipFile.getAbsolutePath());
                }
            }
        }

        if (chips.isEmpty()) {
            chips.add(DEFAULT_CHIP_PATH);
        }

        List<String> result = new ArrayList<>(chips);
        result.sort(String::compareTo);
        return result;
    }

    /**
     * Get chip information
     */
    public ChipInfo getChipInfo(String chipPath) {
        Pointer chip = null;
        Pointer chipInfo = null;

        try {
            chip = LibGpiodNative.INSTANCE.gpiod_chip_open(chipPath);
            if (chip == null) {
                return null;
            }

            chipInfo = LibGpiodNative.INSTANCE.gpiod_chip_get_info(chip);
            if (chipInfo == null) {
                return null;
            }

            String name = LibGpiodNative.INSTANCE.gpiod_chip_info_get_name(chipInfo);
            String label = LibGpiodNative.INSTANCE.gpiod_chip_info_get_label(chipInfo);
            long numLines = LibGpiodNative.INSTANCE.gpiod_chip_info_get_num_lines(chipInfo);

            return new ChipInfo(chipPath, name, label, (int) numLines);

        } finally {
            if (chipInfo != null) {
                LibGpiodNative.INSTANCE.gpiod_chip_info_free(chipInfo);
            }
            if (chip != null) {
                LibGpiodNative.INSTANCE.gpiod_chip_close(chip);
            }
        }
    }

    /**
     * Clear pin cache - useful for testing or when pin configurations change
     */
    public void clearCache() {
        this.pinCache.clear();
    }

    /**
     * Refresh pin discovery - rescans all chips
     */
    public void refresh() {
        this.initialized = false;
        clearCache();
        initialize();
    }

    /**
     * Create cache key for pin instances
     */
    private String createCacheKey(String identifier, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        return identifier + "|" + direction + "|" + mode + "|" + trigger;
    }

    /**
     * Information about a GPIO chip
     */
    public static class ChipInfo {

        private final String path;
        private final String name;
        private final String label;
        private final int numLines;

        public ChipInfo(String path, String name, String label, int numLines) {
            this.path = path;
            this.name = name;
            this.label = label;
            this.numLines = numLines;
        }

        public String getPath() {
            return this.path;
        }

        public String getName() {
            return this.name;
        }

        public String getLabel() {
            return this.label;
        }

        public int getNumLines() {
            return this.numLines;
        }

        @Override
        public String toString() {
            return String.format("ChipInfo{path='%s', name='%s', label='%s', numLines=%d}", this.path, this.name,
                    this.label, this.numLines);
        }
    }
}
