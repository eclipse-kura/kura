# Eclipse Kura User Documentation

We're using several tools for building Kura User Documentation:
- [mkdocs](https://www.mkdocs.org/): the main tool to generate the static website
- [mkdocs-material](https://github.com/squidfunk/mkdocs-material): the used material-style theme
- [mike](https://github.com/jimporter/mike): the versioning tool, based on mkdocs

### Environment setup

In order to install mkdocs, you need to have [Python](https://www.python.org/) >= 3.8 and [pip](https://github.com/pypa/get-pip) installed.

The required dependencies for building the documentation can be found in the root folder `requirement.txt` file. For installing them we suggest using a [Python Virtual Environment](https://docs.python.org/3/library/venv.html).

Create a virtual environment as follows:

```bash
python3 -m venv .venv
```

this will create a folder in you current path named `.venv`. Activate the virtual environment with:

```bash
source .venv/bin/activate
```

you're now using the virtual environment, update `pip` and install the required dependencies.

```bash
pip3 install --upgrade pip
```

```bash
pip3 install -r requirements.txt
```
