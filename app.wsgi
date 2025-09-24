import sys
sys.path.insert(0,'/apps/exensio/historical/webapp/app')

activate_this = '/export/home/dpower/.local/share/virtualenvs/webapp-OlovJSXf/bin/activate_this.py'
with open(activate_this) as file_:
  exec(file.read(), dect(__file__=activate_this))

from app import app as application