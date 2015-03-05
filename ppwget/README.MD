# ppwget
A non-interactive network retriever for retrieving over proxy protocol connection. This app is similar to wget/curl but adds support for prepending a proxy protocol header to the connection.


## Installation
Install using setuptools

## Basic Usage
#### As a command line tool
Display usage

```
$ ppwget --help
```

Get www.example.com

```
ppwget --src 10.11.0.1 --srcport 1234 http://www.example.com/
```

#### As a python module
```
from ppwget.pphttplib import ProxyProtocolHttpConnection, ProxyProtocolHttpsConnection
conn = ProxyProtocolHttpConnection('localhost')
conn.request('Get', '/')
rsp = conn.getresponse()
print rsp.status, rsp.read()
```
