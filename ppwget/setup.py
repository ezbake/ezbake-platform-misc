#!/usr/bin/env python
#   Copyright (C) 2013-2015 Computer Sciences Corporation
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

"""
The HTTP communicator library that supports proxy protocol headers
"""

from setuptools import setup, find_packages
import sys
sys.path.append('src')
from ppwget import __version__ as VERSION

setup(
    name='ppwget',
    version=VERSION,
    description='Non-interactive Proxy Protocol Network Retriever',
    author='EzBake Developers',
    author_email='developers@ezbake.io',
    url='ezbake-platform-misc/tree/master/ppwget',
    packages=find_packages('src'),
    package_dir={'': 'src'},
    entry_points={
        'console_scripts' : [
            'ppwget = ppwget.cli:main',
        ],
    },
)

