---
layout: page
title:  "System Testing"
categories: [qa]
---

QA Procedure
------------

1. Download the desired build from [Hudson CI](https://hudson.eclipse.org/kura/)
1. Checkout the latest [QA spreadsheet]({{ site.baseurl }}/assets/qa/system-test/qa_kura.ods), make a copy and [name it properly](#file-naming-convention).
1. Fill-in the details in the first ~20 rows
1. Execute the tests
   1. Run the test on the target platform
   1. Mark Pass (Y/N) column with Y(es)/N(o)/P(artial)
   1. Report found issues on GitHub and fill-in Bug/Additional info:
      ```
      <No>) #<GitHub issue number>
      Issue description

      e.g.

      1) #1111
      Issue description...
      2) #1112
      ...
      ```
   1. Add issue number to appropriate Build number column.
1. Place the results into `assets/qa/system-test/results/<version>/` directory.

When verifying the fixed issues, fill-in the appropriate Build number column with issue status e.g.
`#1111 - verified`

Office suite support
--------------------

There are several commercial office suites and their free alternatives available. It is recommended to use the open
document format - ods - with varying degrees of support in these office suites.

The best option that preserves most spreadsheet functionality is to open and save the file in LibreOffice 5.2+.

Second best option is to open and save the file in OpenOffice 4.1+. Here only some conditional formatting will not show.

Excel 2010 doesn't open/repair the file properly, so formulas will be lost and replaced with values. OpenOffice 4.0 doesn't
handle all formulas/conditional formatting properly either - errors may be shown instead.

File Naming Convention
----------------------

Copy and rename [qa_kura.ods]({{ site.baseurl }}/assets/qa/system-test/qa_kura.ods) this way:

`qa_kura_<version>_<installer name>[_<name or initials>].ods`

e.g.

`qa_kura_3.0.0-M1_raspberry-pi-3_installer.ods`

or

`qa_kura_3.0.0-M1_raspberry-pi-3_installer_ME.ods`

Environment
-----------

### Hardware

* Raspberry Pi
    * B
    * B+
    * 2
    * 3
* BeagleBone Black

### OS

* Raspbian
* Fedora PI

### Java

* Oracle JDK 1.8
* Open JDK 1.8