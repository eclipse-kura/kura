package com.eurotech.denali.driver.processor;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class WebDriverActions extends DriverProcessor {

	public void click(By by) {
		waitUntilNotBusy();
		waitUntilElementVisible(by);
		driver.findElement(by).click();
	}
	
	public void sendKeys(By by, String value) {
		waitUntilNotBusy();
		waitUntilElementVisible(by);
		WebElement element = driver.findElement(by);
		element.clear();
		element.sendKeys(Keys.CONTROL.toString(), "a");
		element.sendKeys(Keys.BACK_SPACE.toString());
		element.sendKeys(value);
	}
	
	public String getText(By by) {
		waitUntilNotBusy();
		return driver.findElement(by).getText();
	}
	
	public String getValue(By by) {
		waitUntilNotBusy();
		return driver.findElement(by).getAttribute("value");
	}

	public String getText(WebElement element) {
		waitUntilNotBusy();
		return element.getText();
	}

	public List<WebElement> getWebElements(By by) {
		waitUntilNotBusy();
		waitUntilElementVisible(by);
		return driver.findElements(by);
	}

	public void get(String url) {
		driver.get(url);
	}

	public void waitUntilNotBusy() {
		try {
			wait.until(ExpectedConditions.invisibilityOfElementLocated(By
					.xpath("//*[contains(text(),'Applying...')]")));
			wait.until(ExpectedConditions.invisibilityOfElementLocated(By
					.xpath("//*[contains(text(),'Loading')]")));
			wait.until(ExpectedConditions.invisibilityOfElementLocated(By
					.xpath("//*[contains(text(),'Waiting..')]")));
		} catch (TimeoutException e) {
			throw new RuntimeException(e);
		}
	}

	public void waitUntilElementVisible(By by) {
		try {
			wait.until(ExpectedConditions.visibilityOfElementLocated(by));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isDisplayed(By by) {
		waitUntilNotBusy();
		return isPresent(by);
	}
	
	public boolean isPresent(By by) {
		try {
			return driver.findElement(by).isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

	public String getAttribute(By by, String attributeName) {
		waitUntilNotBusy();
		return driver.findElement(by).getAttribute(attributeName);
	}

	public boolean isButtonEnabled(By by) {
		waitUntilNotBusy();
		boolean status = true;
		try {
			String result = driver.findElement(by).getCssValue("cursor");
			if (!result.contains("pointer") && !result.contains("auto")) {
				status = false;
			}
		} catch (Exception e) {
			status = false;
		}
		return status;
	}
	
	public boolean isTextBoxEnabled(By by) {
		waitUntilNotBusy();
		boolean status = true;
		try {
			String result = driver.findElement(by).getCssValue("cursor");
			if (!result.contains("auto") && !result.contains("text")) {
				status = false;
			}
		} catch (Exception e) {
			status = false;
		}
		return status;
	}
}