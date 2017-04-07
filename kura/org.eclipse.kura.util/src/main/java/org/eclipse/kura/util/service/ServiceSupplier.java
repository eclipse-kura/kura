/*******************************************************************************
 * Copyright (c) 2017 Amit Kumar Mondal
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.util.service;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.kura.util.service.ServiceUtil.getBundleContext;
import static org.eclipse.kura.util.service.ServiceUtil.getServiceReferences;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.UtilMessages;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ServiceSupplier} is used to safely retrieve target service
 * instances. The release of target service objects can be performed
 * automatically if used with {@code try-with-resources} block. Otherwise
 * {@link #close()} must be invoked that releases the service objects and
 * associated {@link ServiceTracker}. it tries to follow the best practice
 * guidelines inherently that developers don't have to worry at all. We can also
 * make sure that the source base would be free from resource and memory leak
 * interferences and it could also work in concurrent programming environment
 * safely. <br/>
 * <br/>
 * <b>Usage 1:</b>
 *
 * <pre>
 * try (final ServiceSupplier{@code <MyService>} serviceSupplier = ServiceSupplier.supply(MyService.class, filter)) {
 * 	   final Stream{@code <MyService>} stream = serviceSupplier.get();
 * }
 * </pre>
 *
 * <br/>
 * <b>Usage 2:</b>
 *
 * <pre>
 * final Collection<ServiceReference{@code <MyService>} refs = ServiceSupplier.references(MyService.class, filter)
 * 		.collect(Collectors.toCollection());
 * for (final ServiceReference{@code <MyService>} ref : refs) {
 * 	try (final ServiceSupplier serviceSupplier = ServiceSupplier.supply(ref)) {
 * 		final Stream{@code <MyService>} stream = serviceSupplier.get();
 * 		final Optional{@code <MyService>} service = stream.findFirst();
 * 	}
 * }
 * </pre>
 *
 * <br/>
 * <b>Usage 3:</b>
 *
 * <pre>
 * final TrackerSupplier{@code <MyService>} trackerSupplier = ServiceSupplier.supplyWithTracker(MyService.class, filter);
 *
 * final BiConsumer{@code <ServiceReference<MyService>, MyService>} onAddedAction = (MyService::doA).andThen(MyService::doB);
 * final BiConsumer{@code <ServiceReference<MyService>, MyService>} onRemovalAction = (MyService::removeA).andThen(MyService::removeB);
 * final BiConsumer{@code <ServiceReference<MyService>, MyService>} onModifiedAction = MyService::update;
 *
 * final ServiceCallback{@code <MyService>} callback = ServiceCallbackSupplier.<MyService>create()
 * 								   .onAdded(onAddedAction)
 * 								   .onModified(onModifiedAction)
 * 								   .onRemoved(onRemovalAction)
 * 								   .get();
 *
 * final ServiceSupplier{@code <MyService>} serviceSupplier = trackerSupplier.shouldWait(false).withCallback(callback).get();
 * final Stream{@code <MyService>} stream = serviceSupplier.get();
 *
 * serviceSupplier.close(); //call it when service tracking is not required at all
 * </pre>
 *
 * <br/>
 * <b>Usage 4 (Fluent API):</b>
 *
 * <pre>
 * {@code
 * final ServiceSupplier{@code <MyService>} serviceSupplier = ServiceSupplier.supplyWithTracker(MyService.class, null)
 * 							    .shouldWait(true)
 *							    .timeout(5)
 *							    .timeunit(SECONDS)
 *							    .withCallback(
 *								  ServiceCallbackSupplier
 *								    .{@code <MyService>}create()
 *								    .onAdded((r, s) -> s.doA())
 *								    .onRemoved(MyService::removeA)
 *								    .get())
 *							    .get();
 *
 * final Stream{@code <MyService>} stream = serviceSupplier.get();
 *
 * serviceSupplier.close(); //call it when service tracking is not required at all
 *
 * </pre>
 *
 * @param <T>
 *            the target service object
 */
public final class ServiceSupplier<T> implements Supplier<Stream<T>>, AutoCloseable {

	/** Logger Instance */
	private static final Logger logger = LoggerFactory.getLogger(ServiceSupplier.class);

	/** Localization instance */
	private static final UtilMessages message = LocalizationAdapter.adapt(UtilMessages.class);

	/** Associated Bundle Context */
	private static final BundleContext bundleContext = getBundleContext();

	/** Associated Service References */
	private final Collection<ServiceReference<T>> serviceReferences = new ConcurrentSkipListSet<>();

	/** Acquired Service Instances */
	private final Map<ServiceReference<T>, T> acquiredServices = new ConcurrentHashMap<>();

	/** Flag to check if this supplier instance has already been closed */
	private final AtomicBoolean isClosed = new AtomicBoolean(false);

	/** Flag to check if the associated tracker instance is open */
	private final AtomicBoolean isTrackerOpened = new AtomicBoolean(false);

	/** Tracker to track instead of returning the existing ones */
	private Optional<ServiceTracker<T, T>> serviceTracker = Optional.empty();

	/** Tracker Callback Action */
	private Optional<ServiceCallback<T>> callback = Optional.empty();

	/**
	 * Flag to wait for at least one service to be tracked by the associated
	 * {@link ServiceTracker}
	 */
	private boolean shouldWait;

	/**
	 * The time interval in milliseconds that the associated
	 * {@link ServiceTracker} will wait. Must be greater than or equals to 1.
	 */
	private long timeout;

	/**
	 * The unit of specified timeout for the associated {@link ServiceTracker}
	 */
	private TimeUnit timeUnit;

	/** Constructor */
	private ServiceSupplier(final ServiceReference<T> reference) {
		requireNonNull(reference, message.referenceNonNull());
		this.serviceReferences.add(reference);
	}

	/** Constructor */
	private ServiceSupplier(final Class<T> target, final String filter, final boolean isTracker,
			final boolean shouldWait, final long timeout, final TimeUnit timeUnit,
			final Optional<ServiceCallback<T>> callback) {
		requireNonNull(target, message.targetNonNull());
		if (isTracker) {
			final ServiceTrackerCustomizer<T, T> customizer = new CustomTrackerCustomizer();
			serviceTracker = Optional.of(new ServiceTracker<>(bundleContext, target.getName(), customizer));
			this.shouldWait = shouldWait;
			this.timeout = timeout;
			this.timeUnit = timeUnit;
			this.callback = callback;
		} else {
			final Stream<ServiceReference<T>> refs = getServiceReferences(target, filter).stream();
			this.serviceReferences.addAll(refs.collect(toSet()));
		}
	}

	/**
	 * Supplies the instance of {@link ServiceSupplier} from which the target
	 * service instances can be retrieved
	 *
	 * @param target
	 *            the service instance to retrieve
	 * @param filter
	 *            the valid OSGi service filter (can be {@code null})
	 * @return the {@link ServiceSupplier} instance
	 * @throws NullPointerException
	 *             if {@code target} is {@code null}
	 * @throws IllegalArgumentException
	 *             the provided {@code filter} is not valid
	 * @throws IllegalStateException
	 *             if the {@link BundleContext} instance for this bundle cannot
	 *             be acquired
	 */
	public static <T> ServiceSupplier<T> supply(final Class<T> target, final String filter) {
		return new ServiceSupplier<>(target, filter, false, false, 0, null, null);
	}

	/**
	 * Supplies the instance of {@link ServiceSupplier} from which the target
	 * service instances can be retrieved
	 *
	 * @param target
	 *            the service instance to retrieve
	 * @param filter
	 *            the valid OSGi service filter (can be {@code null})
	 * @return the {@link TrackerSupplier} instance
	 * @throws NullPointerException
	 *             if {@code target} is {@code null}
	 * @throws IllegalArgumentException
	 *             if the provided {@code filter} is not valid
	 */
	public static <T> TrackerSupplier<T> supplyWithTracker(final Class<T> target, final String filter) {
		return new TrackerSupplier<>(target, filter, true);
	}

	/**
	 * Used to configured {@link ServiceTracker} waiting related information
	 *
	 * @param <S>
	 *            the type of service to wait for
	 */
	public static final class TrackerSupplier<T> implements Supplier<ServiceSupplier<T>> {

		private final Class<T> target;
		private final String filter;
		private final boolean isTracker;
		private long timeout = 1;
		private TimeUnit timeUnit = MILLISECONDS;
		private boolean shouldWait;
		private Optional<ServiceCallback<T>> callback;

		/** Constructor */
		private TrackerSupplier(final Class<T> target, final String filter, final boolean isTracker) {
			this.target = target;
			this.filter = filter;
			this.isTracker = isTracker;
		}

		/**
		 * If set to {@code true}, the associated {@code ServiceTracker} waits
		 * for at least one service.
		 *
		 * @param shouldWait
		 *            {@code true} if the {@code ServiceTracker} needs to wait
		 *            for the specified amount of time
		 * @return {@link TrackerSupplier} instance
		 * @see TrackerSupplier#timeout(long)
		 * @see TrackerSupplier#timeunit(TimeUnit)
		 * @see TrackerSupplier#get()
		 */
		public TrackerSupplier<T> shouldWait(final boolean shouldWait) {
			this.shouldWait = shouldWait;
			return this;
		}

		/**
		 * If the associated {@code ServiceTracker} waits for at least one
		 * service, this method can be used to specify the amount of time the
		 * {@code ServiceTracker} should wait before timeout. Ideally you should
		 * also specify the {@link TimeUnit} of this timeout by invoking
		 * {@link TrackerSupplier#timeunit(TimeUnit)}. Otherwise, time unit
		 * would by default be set to {@link TimeUnit#MILLISECONDS} and the
		 * default timeout is set to 1. <br/>
		 * <br/>
		 * <b>N.B</b>: The timeout should be greater than or equals to 1.
		 *
		 * @param timeout
		 *            the specified amount of time
		 * @return {@link TrackerSupplier} instance
		 * @throws IllegalArgumentException
		 *             if timeout value is negative or zero
		 * @see TrackerSupplier#shouldWait(boolean)
		 * @see TrackerSupplier#timeunit(TimeUnit)
		 * @see TrackerSupplier#get()
		 */
		public TrackerSupplier<T> timeout(final long timeout) {
			if (timeout <= 1) {
				throw new IllegalArgumentException(message.timeoutError());
			}
			this.timeout = timeout;
			return this;
		}

		/**
		 * If the associated {@code ServiceTracker} waits for at least one
		 * service, this method can be used to specify the unit of the time as
		 * configured. The default time unit has been set to
		 * {@link TimeUnit#MILLISECONDS}.
		 *
		 * @param timeUnit
		 *            the unit of the specified amount of time (can be
		 *            {@code null})
		 * @return {@link TrackerSupplier} instance
		 * @see TrackerSupplier#shouldWait(boolean)
		 * @see TrackerSupplier#timeout(long)
		 * @see TrackerSupplier#get()
		 */
		public TrackerSupplier<T> timeunit(final TimeUnit timeUnit) {
			if (nonNull(timeUnit)) {
				this.timeUnit = timeUnit;
			}
			return this;
		}

		/**
		 * Customization to the {@link ServiceSupplier} can be provided through
		 * the {@link ServiceCallbackSupplier} class that allows
		 * {@code ServiceSupplier} to customize the tracked service objects
		 * through the instance of {@link ServiceCallback}
		 *
		 * @param callbackBucallbackilder
		 *            the {@link ServiceCallback} instance (can be {@code null})
		 * @return {@link TrackerSupplier} instance
		 * @see TrackerSupplier#get()
		 * @see ServiceCallback
		 */
		public TrackerSupplier<T> withCallback(final ServiceCallback<T> callback) {
			this.callback = Optional.ofNullable(callback);
			return this;
		}

		/**
		 * Creates an instance of {@link ServiceSupplier} with the provided
		 * {@link ServiceTracker} related configurations
		 *
		 * @throws NullPointerException
		 *             if {@code target} is null
		 * @throws IllegalStateException
		 *             if the {@link BundleContext} instance for this bundle
		 *             cannot be acquired
		 * @return {@link ServiceSupplier} instance
		 */
		@Override
		public ServiceSupplier<T> get() {
			return new ServiceSupplier<>(target, filter, isTracker, shouldWait, timeout, timeUnit, callback);
		}
	}

	/**
	 * The {@link ServiceCallback} interface allows a {@link ServiceSupplier} to
	 * customize the service objects that are tracked. A {@link ServiceCallback}
	 * is called when a service is being added to a {@link ServiceSupplier}. The
	 * {@link ServiceCallback} can then return an object for the tracked
	 * service. A {@link ServiceCallback} is also called when a tracked service
	 * is modified or has been removed from a {@code ServiceSupplier}. <br/>
	 * <br/>
	 * <b>N.B</b>: Make use of {@link ServiceCallbackSupplier} to create an
	 * instance of {@link ServiceCallback}
	 *
	 * @param <T>
	 *            The type of the service being tracked
	 * @see ServiceCallbackSupplier
	 */
	public interface ServiceCallback<T> {

		/**
		 * This method is called before a service which matched the search
		 * parameters of the {@link ServiceSupplier} is added to the
		 * {@link ServiceSupplier}
		 *
		 * @return the {@link BiConsumer} action to be performed wrapped in
		 *         {@link Optional} instance
		 */
		public Optional<BiConsumer<ServiceReference<T>, T>> onAdded();

		/**
		 * This method is called before a service which matched the search
		 * parameters of the {@link ServiceSupplier} is modified
		 *
		 * @return the {@link BiConsumer} action to be performed wrapped in
		 *         {@link Optional} instance
		 */
		public Optional<BiConsumer<ServiceReference<T>, T>> onModified();

		/**
		 * This method is called before a service which matched the search
		 * parameters of the {@link ServiceSupplier} is removed from the
		 * {@link ServiceSupplier}
		 *
		 * @return the {@link BiConsumer} action to be performed wrapped in
		 *         {@link Optional} instance
		 */
		public Optional<BiConsumer<ServiceReference<T>, T>> onRemoved();

	}

	/**
	 * The {@link ServiceCallbackSupplier} interface allows a consumers to
	 * create {@link ServiceCallback} instances using fluent API paradigms.
	 *
	 * @param <T>
	 *            The type of the service being tracked
	 * @see ServiceCallback
	 */
	public static final class ServiceCallbackSupplier<T> implements Supplier<ServiceCallback<T>> {

		/** Callback Actions */
		private BiConsumer<ServiceReference<T>, T> onAddedAction;
		private BiConsumer<ServiceReference<T>, T> onModifiedAction;
		private BiConsumer<ServiceReference<T>, T> onRemovedAction;

		/** Constructor */
		private ServiceCallbackSupplier() {
		}

		/** Create an instance of {@link ServiceCallbackSupplier} */
		public static <T> ServiceCallbackSupplier<T> create() {
			return new ServiceCallbackSupplier<>();
		}

		/**
		 * This method is called before a service which matched the search
		 * parameters of the {@link ServiceSupplier} is added to the
		 * {@link ServiceSupplier}
		 *
		 * @param onAddedAction
		 *            the action to be executed on addition of the service
		 * @see ServiceCallbackSupplier#onModified(BiConsumer)
		 * @see ServiceCallbackSupplier#onRemoved(BiConsumer)
		 * @see ServiceCallbackSupplier#get()
		 */
		public ServiceCallbackSupplier<T> onAdded(final BiConsumer<ServiceReference<T>, T> onAddedAction) {
			this.onAddedAction = onAddedAction;
			return this;
		}

		/**
		 * This method is called when a service being tracked by the
		 * {@link ServiceSupplier} has had it properties modified.
		 *
		 * @param onModifiedAction
		 *            the action to be executed on modification of the service
		 * @see ServiceCallbackSupplier#onAdded(BiConsumer)
		 * @see ServiceCallbackSupplier#onRemoved(BiConsumer)
		 * @see ServiceCallbackSupplier#get()
		 */
		public ServiceCallbackSupplier<T> onModified(final BiConsumer<ServiceReference<T>, T> onModifiedAction) {
			this.onModifiedAction = onModifiedAction;
			return this;
		}

		/**
		 * This method is called after a service is no longer being tracked by
		 * the {@link ServiceTracker}.
		 *
		 * @param onRemovedAction
		 *            the action to be executed on removal of the service
		 * @return {@link ServiceCallback} instance
		 * @see ServiceCallbackSupplier#onAdded(BiConsumer)
		 * @see ServiceCallbackSupplier#onModified(BiConsumer)
		 * @see ServiceCallbackSupplier#get()
		 */
		public ServiceCallbackSupplier<T> onRemoved(final BiConsumer<ServiceReference<T>, T> onRemovedAction) {
			this.onRemovedAction = onRemovedAction;
			return this;
		}

		/**
		 * Returns the instance of {@link ServiceCallback}
		 *
		 * @return the instance of {@link ServiceCallback}
		 */
		@Override
		public ServiceCallback<T> get() {
			return new ServiceCallback<T>() {

				/** {@inheritDoc} */
				@Override
				public Optional<BiConsumer<ServiceReference<T>, T>> onAdded() {
					return Optional.ofNullable(onAddedAction);
				}

				/** {@inheritDoc} */
				@Override
				public Optional<BiConsumer<ServiceReference<T>, T>> onModified() {
					return Optional.ofNullable(onModifiedAction);
				}

				/** {@inheritDoc} */
				@Override
				public Optional<BiConsumer<ServiceReference<T>, T>> onRemoved() {
					return Optional.ofNullable(onRemovedAction);
				}

			};
		}
	}

	/**
	 * Custom {@link ServiceTrackerCustomizer} instance
	 */
	private final class CustomTrackerCustomizer implements ServiceTrackerCustomizer<T, T> {

		/** {@inheritDoc} */
		@Override
		public synchronized T addingService(final ServiceReference<T> reference) {
			final T service = bundleContext.getService(reference);
			acquireService(reference);
			callback.ifPresent(c -> c.onAdded().ifPresent(r -> r.accept(reference, service)));
			return service;
		}

		/** {@inheritDoc} */
		@Override
		public synchronized void modifiedService(final ServiceReference<T> reference, final T service) {
			callback.ifPresent(c -> c.onModified().ifPresent(r -> r.accept(reference, service)));
		}

		/** {@inheritDoc} */
		@Override
		public synchronized void removedService(final ServiceReference<T> reference, final T service) {
			releaseService(reference);
			callback.ifPresent(c -> c.onRemoved().ifPresent(r -> r.accept(reference, service)));
			bundleContext.ungetService(reference);
		}

		/**
		 * Releases the service instance and its {@link ServiceReference}
		 * instance and removes it from the associated {@link Map} instance
		 *
		 * @param reference
		 *            the {@link ServiceReference} instance
		 */
		private void releaseService(final ServiceReference<T> reference) {
			if (nonNull(reference)) {
				acquiredServices.remove(reference);
			}
		}
	}

	/**
	 * Supplies the instance of {@link ServiceSupplier} from which the target
	 * service instance can be retrieved
	 *
	 * @param reference
	 *            the {@link ServiceReference} instance to acquire the target
	 *            service instance
	 * @return the {@link ServiceSupplier} instance
	 * @throws NullPointerException
	 *             if the provided {@link ServiceReference} is {@code null}
	 */
	public static <T> ServiceSupplier<T> supply(final ServiceReference<T> reference) {
		return new ServiceSupplier<>(reference);
	}

	/**
	 * Retrieves the {@link Stream} of target service instances. If the consumer
	 * of this class uses a tracker to track or monitor target service instances
	 * dynamically, this method is capable of returning the service instances
	 * that are tracked so far. This method will be blocked for the specified
	 * amount of time if the consumer of this class needs to have the associated
	 * {@link ServiceTracker} to wait for at least one service instance to be
	 * available before the timeout occurs.
	 *
	 * @throws IllegalStateException
	 *             if {@link ServiceSupplier} resource is already closed or the
	 *             {@link BundleContext} with which the {@link ServiceTracker}
	 *             gets created is no longer valid.
	 * @return the {@link Stream} of acquired service instances
	 */
	@Override
	public Stream<T> get() {
		if (this.isClosed.get()) {
			throw new IllegalStateException(message.alreadyClosed());
		}
		serviceTracker.ifPresent(t -> {
			if (isTrackerOpened.compareAndSet(false, true)) {
				t.open();
			}
			if (shouldWait) {
				final long timeoutInMillis = timeUnit.toMillis(timeout);
				try {
					t.waitForService(timeoutInMillis);
				} catch (final InterruptedException e) {
					// not required
				}
			}
			final ServiceReference<T>[] refs = t.getServiceReferences();
			if (nonNull(refs)) {
				serviceReferences.addAll(Arrays.stream(refs).collect(Collectors.toList()));
			}
		});
		try {
			this.serviceReferences.stream().filter(this::isNotAcquired).forEach(this::acquireService);
		} catch (final Exception ex) {
			logger.error(message.errorRetrievingService(), ex);
		}
		return this.acquiredServices.values().stream();
	}

	/**
	 * Checks if a service instance is not yet acquired from the provided
	 * {@link ServiceReference} instance. Returns {@code false} if
	 * {@code #acquiredServices} instance contains a mapping for the specified
	 * {@link ServiceReference}. More formally, returns {@code false} if and
	 * only if {@code #acquiredServices} instance contains a mapping for a
	 * {@link ServiceReference} {@code s} such that {@code key.equals(s)}.
	 *
	 * Every service registered in the Framework has a unique
	 * {@code ServiceRegistration} object and may have multiple, distinct
	 * {@code ServiceReference} objects referring to it.
	 * {@code ServiceReference} objects associated with a
	 * {@code ServiceRegistration} object have the same {@code hashCode} and are
	 * considered equal (more specifically, their {@code equals()} method will
	 * return {@code true} when compared).
	 *
	 * @param reference
	 *            the {@link ServiceReference} instance
	 * @return true if not yet acquired, otherwise false
	 */
	private boolean isNotAcquired(final ServiceReference<T> reference) {
		return !this.acquiredServices.containsKey(reference);
	}

	/**
	 * Acquire the service instance from the specified {@link ServiceReference}
	 * instance and adds it to the associated {@link Map} instance
	 *
	 * @param reference
	 *            the {@link ServiceReference} instance
	 */
	private void acquireService(final ServiceReference<T> reference) {
		final T service = bundleContext.getService(reference);
		if (nonNull(service)) {
			this.acquiredServices.put(reference, service);
		}
	}

	/**
	 * Releases the service objects for the service referenced by the
	 * {@link ServiceReference} instances as stored in the associated
	 * {@link Map}. This is an idempotent operation, that is, Idempotent means
	 * that you can apply this operation a number of times, but the resulting
	 * state of one call will be indistinguishable from the resulting state of
	 * multiple calls.
	 */
	@Override
	public void close() {
		if (!this.isClosed.compareAndSet(false, true)) {
			return;
		}
		// close the tracker if and only if it is open. Multiple calls to
		// close() without invoking get() will still perform as expected
		if (isTrackerOpened.compareAndSet(true, false)) {
			serviceTracker.ifPresent(t -> t.close());
		}
		if (this.acquiredServices.isEmpty()) {
			return;
		}
		for (final Entry<ServiceReference<T>, T> service : this.acquiredServices.entrySet()) {
			final ServiceReference<T> ref = service.getKey();
			try {
				bundleContext.ungetService(ref);
			} catch (final Exception ex) {
				logger.warn(message.closeFailed(), ex);
			}
		}
		this.acquiredServices.clear();
		this.serviceReferences.clear();
	}

	/**
	 * This is not a good practice though but in case of this, it is very much
	 * needed because we are not sure how consumers are going to use this class.
	 * If they don't close the resources, it will keep on listening to the
	 * tracked service forever. So, it is better to have trackers and consumed
	 * services dereferenced while finalizing all its references. Even though it
	 * is not guaranteed that the reference will be garbage collected at a
	 * certain point of time, it is an advise to use it as it is better late
	 * than never.
	 */
	@Override
	protected void finalize() throws Throwable {
		close();
	}

}