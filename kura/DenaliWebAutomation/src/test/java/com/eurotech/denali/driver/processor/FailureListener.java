package com.eurotech.denali.driver.processor;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.TestListenerAdapter;

import com.eurotech.denali.common.Application;

public class FailureListener extends TestListenerAdapter {

	@Override
	public void onTestFailure(ITestResult result) {
		if (!result.isSuccess()) {
			WebDriver driver = DriverProcessor.getDriver();
			if (driver == null) {
				return;
			}
			File scrFile = ((TakesScreenshot) driver)
					.getScreenshotAs(OutputType.FILE);
			DateFormat dateFormat = new SimpleDateFormat(
					"dd_MMM_yyyy__hh_mm_ssaa");
			String destDir = Application.getCurrentLocation() + File.separator
					+ "target" + File.separator + "screenshot";
			new File(destDir).mkdirs();
			String destFile = dateFormat.format(new Date()) + ".png";

			File imageLocation = new File(destDir + File.separator + destFile);
			try {
				FileUtils.copyFile(scrFile, imageLocation);
			} catch (IOException e) {
				e.printStackTrace();
			}
			Reporter.setEscapeHtml(false);
			String screenPath = "Saved <a href=" + imageLocation
					+ ">Screenshot</a>";
			Reporter.log(screenPath);
		}
	}
}
