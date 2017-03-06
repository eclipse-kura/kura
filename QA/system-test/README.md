Eclipse KURA QA
===============

QA activities currently comprise manual system testing. The test procedures are described in the `qa_kura.xlsx`, here.

System testing
==============

QA Procedure
------------
1. Download the desired build from [Hudson CI](https://hudson.eclipse.org/kura/)
1. Checkout the latest QA spreadsheet.
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
1. Place the results into `results/<version>/` directory.

When verifying the fixed issues, fill-in the appropriate Build number column with issue status e.g.
`#1111 - verified`

File Naming Convention
----------------------

Copy and rename `qa_kura.xlsx` this way:
`qa_kura_<version>_<installer name>.xlsx`

e.g.
`qa_kura_3.0.0-M1_raspberry-pi-3_installer.xlsx`

Environment
-----------

### Hardware

* Raspberry Pi 1, 2, 3
* BeagleBone Black


### OS

* Raspbian
* Fedora PI

### Java

* Oracle JDK 1.8
* Open JDK 1.8