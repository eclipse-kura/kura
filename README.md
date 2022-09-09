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

You're now ready do build the documentation sources locally.

### Building the docs

#### Simple build

MkDocs comes with a built-in dev-server that lets you preview your documentation as you work on it. Make sure you're in the same directory as the `mkdocs.yml` configuration file and you have activated the Python virtual environment, then start the server by running:

```bash
mkdocs serve
```

Open up [http://127.0.0.1:8000/](http://127.0.0.1:8000/) in your browser, and you'll see the docs built from your current sources.

The MkDocs dev-server also supports auto-reloading, and will rebuild your documentation whenever anything in the configuration file, documentation directory, or theme directory changes.

#### Complete build

`mike` is the tool to use for building and versioning the documentation. Modify the markdown files and edit `mkdocs.yml` file accordingly.

To build the documentation and see the changes on your local machine:

```bash
mike deploy [version]
```

Usually `[version]` should match the branch you're on. Let's say you're updating the documentation for `docs-develop` you'll the need to run `mike deploy docs-develop`. This command will create a folder in you current path names `site`, here you'll find the built documentation sources. You can serve them through a local webserver using:

```bash
mike serve
```

> **Note**: If you can't see your docs it might be because you haven't set a default version to serve. Run the following command for setting the default version to be served by `mike`:
> ```bash
> mike set-default [version]
> ```
> where `[version]` is the one you used in the `deploy` command before.
