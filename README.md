## Getting Started

- Make sure to have python installed.

On MAC
```
sudo apt install python3.8
```

On Ubuntu
```Bash
brew install python@3.8
```

- Create the virtual environment in the root directory of your project.
```Bash
python3 -m venv venv
```

- Activate the virtual environment
```Bash
source venv/bin/activate
```

- install the requirements needed
```
pip install mkdocs-material
pip install "mkdocs-material[imaging]"
```

- Start the documentation.
```Bash
mkdocs serve
```
