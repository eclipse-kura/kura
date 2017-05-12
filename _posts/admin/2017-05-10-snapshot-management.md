---
layout: page
title:  "Snapshot Management"
categories: [admin]
---

The overall configuration of Kura is stored in an XML file called a snapshot. This file includes all of the parameters for every service running in Kura. The original configuration file is named **snapshot_0.xml**. This section describes how snapshots may be used.

Each time a configuration change is made to one of the Kura components, a new XML file is created using the naming convention **snapshot_[time as a long integer].xml**. The nine most recent snapshots are saved, as well as the original snapshot 0.

## How to Access Snapshots
To display snapshots using the [Gateway Administration Console](console.html), select **Settings** from the **System** area, and then click on the **Snapshots** tab. The following three operations are available: **Download**, **Upload and Apply**, and **Rollback**.

![]({{ site.baseurl }}/assets/images/admin/snapshots.png)

## How to Use Snapshots

### Download

The **Download** option provides the ability to save a snapshot XML file onto your computer. This file may then be edited, uploaded back to the device, or transferred to another equivalent device.

### Upload and Apply

The **Upload and Apply** option  provides the ability to import an XML file from your computer and upload it onto the device. This function updates every service in Kura with the parameters defined in the XML file.

{% include alerts.html message='Carefully select the file to be uploaded. An incorrect file may crash Kura and make it unresponsive.' %}

![]({{ site.baseurl }}/assets/images/admin/snapshotsUpload.png)

### Rollback

The **Rollback** option provides the ability to restore the system to a previous configuration.

![]({{ site.baseurl }}/assets/images/admin/snapshotsRollback.png)
