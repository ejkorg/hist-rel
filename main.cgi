#/var/www/html
#!/usr/bin/python
import sys
sys.path.insert(0, '/export/home/dpower/.local/lib/python3.6/site-packages')from wsgiref.handlers import CGIHandler
from myapp import app
CGIHandler().run(app)