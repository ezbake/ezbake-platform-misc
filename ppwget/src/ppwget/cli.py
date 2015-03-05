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

"""Proxy Protocol CLI

A non-interactive network retriever for retrieving over proxy protocol connection 
"""

from __future__ import print_function
import os
import sys
import argparse
import datetime
import ssl
import socket
from urlparse import urlparse
from ppwget.pphttplib import ProxyProtocolHttpConnection, ProxyProtocolHttpsConnection
from ppwget import __version__ as VERSION
from ppwget import __name__ as APP_NAME



MAX_REDIRECTS = 20
NON_REDIRECT_CODES = [304, 305]



def retreiveContent(args, redirect_count=0):
    """Retrieve the content from the network"""

    #restrain redirects
    if redirect_count >= MAX_REDIRECTS:
        print('-- Too many redirects. Aborting!!\n')
        return

    if args.verbose:
        print('%s %s' % (datetime.datetime.now().strftime('--%Y-%m-%d %H:%M:%S--'),
                         args.url.geturl()))

    resource = args.url.geturl().replace('%s://%s' % (args.url.scheme, args.url.netloc), '')

    #determine if secure connection required
    if args.url.scheme == 'https' or args.url.port == 443:
        print('Connecting securely to \'%s\' and requesting \'%s\'' % (args.url.hostname, resource))
        conn = ProxyProtocolHttpsConnection(host=args.url.hostname,
                                            port=443 if not args.url.port else args.url.port,
                                            key_file=args.private_key,
                                            cert_file=args.certificate,
                                            ca_certs=args.ca_certificate,
                                            validate=not args.no_check_certificate,
                                            proxied_src=args.src,
                                            proxied_dst=args.dst,
                                            proxied_src_port=args.srcport,
                                            proxied_dst_port=args.dstport)
    else: #http
        print('Connecting to \'%s\' and requesting \'%s\'' % (args.url.hostname, resource))
        conn = ProxyProtocolHttpConnection(host=args.url.hostname,
                                           port=args.url.port,
                                           proxied_src=args.src,
                                           proxied_dst=args.dst,
                                           proxied_src_port=args.srcport,
                                           proxied_dst_port=args.dstport)

    #initiate request
    requestHeaders = {'User-Agent': '%s/%s' % (APP_NAME, VERSION)}
    conn.request('GET', '%s' % resource, headers=requestHeaders)
    response = conn.getresponse()
    print('%d - %s' % (response.status, response.reason))
    
    if (300 <= response.status <= 399) and (response.status not in NON_REDIRECT_CODES):
        #handle redirects
        redirect = response.getheader('location')
        if redirect:
            print('Redirecting to %s' % redirect)
            args.url = urlparse(redirect)
            retreiveContent(args, redirect_count=(redirect_count + 1))
            return
    else:
        if args.verbose:
            print('HEADERS:\n  %s' % (str(response.msg).replace('\n', '\n  ')))
    
        if response.status == 200:
            if args.url.path and not args.url.path.endswith('/'):
                contentFileName = os.path.basename(args.url.path)
            else:
                #root path retrieval - save as index.html
                contentFileName = 'index.html'

            contentLength = response.getheader('content-length')
            if contentLength:
                contentLength = int(contentLength)
                blocksize = contentLength / 100
                with open(contentFileName, 'wb') as f:
                    sys.stdout.write("\r-- 0%%")
                    sys.stdout.flush()
                    for i in range(101):
                        f.write(response.read(blocksize))
                        sys.stdout.write("\r-- %d%%" % i)
                        sys.stdout.flush()
                sys.stdout.write('\n')
                sys.stdout.flush()
            else:
                print('-- ...')
                with open(contentFileName, 'wb') as f:
                    f.write(response.read())

            print('Saved content as %s' % contentFileName)


def _validateArgs(args):
    """
    Validates the command line arguments
    """
    allowedSchemes = ['http', 'https']
    
    if args.url.scheme:
        if args.url.scheme not in allowedSchemes:
            raise ValueError('URL scheme \'%s\' is not allowed.'
                             ' Only \'%s\' schemes are allowed' % (args.url.scheme, ' '.join(allowedSchemes)))
    else:
        raise ValueError('URL scheme is required.')

    if args.url.scheme == 'https' or args.url.port == 443:
        if args.certificate and not os.access(args.certificate , os.R_OK):
            raise IOError('Unable to read certificate file %s' % args.certificate)
        if args.private_key and not os.access(args.private_key, os.R_OK):
            raise IOError('Unable to read private key file %s' % args.private_key)
        if args.ca_certificate and not os.access(args.ca_certificate, os.R_OK):
            raise IOError('Unable to read CA certificate file %s' % args.ca_certificate)
    
    if not args.url.hostname:
        raise ValueError('URL doesn\'t specify a host')

    return args


def _parseArgs():
    """
    Parse command line for options and positional arguments
    """
    parser = argparse.ArgumentParser(description='A non-interactive network retriever for retrieving over proxy protocol connection',
                                     version='%(prog)s v' + '%s' % VERSION)
    parser.add_argument('-V', '--verbose', action='store_true', help='be very verbose')

    parser.add_argument('url', metavar='<url>', type=urlparse, help='URL to retrieve')

    ppparser = parser.add_argument_group('Proxy Protocol options')
    ppparser.add_argument('--src', default='localhost', help='layer 3 source address')
    ppparser.add_argument('--dst', default='localhost', help='layer 3 destination address')
    ppparser.add_argument('--srcport', type=int, help='layer 3 source port')
    ppparser.add_argument('--dstport', type=int, help='layer 3 destination port')

    sslparser = parser.add_argument_group('HTTPS (SSL/TLS) options')
    sslparser.add_argument('--no-check-certificate', action='store_true', help='don\'t validate the server\'s certificate')
    sslparser.add_argument('--certificate', help='client certificate file in PEM format')
    sslparser.add_argument('--private-key', help='client private key file in PEM format')
    sslparser.add_argument('--ca-certificate', help='file with the bundle of CA\'s')

    return parser.parse_args()


def main():
    """Main entry"""
    exit_code = 0

    try:
        cmd_args = _validateArgs(_parseArgs())
        retreiveContent(cmd_args)
    except ssl.SSLError as ex:
        print('ERROR: unable to establish secure connection to host: ', str(ex), file=sys.stderr)
        exit_code = 1
    except (socket.timeout, socket.error) as ex:
        print('ERROR: connection ', str(ex), file=sys.stderr)
        exit_code = 1
    except (socket.gaierror, socket.herror, IOError, ValueError) as ex:
        print('ERROR: ', str(ex), file=sys.stderr)
        exit_code = 1
    except KeyboardInterrupt:
        print('\nQuit!')
    finally:
        sys.exit(exit_code)


######
if __name__ == '__main__':
    sys.exit(main())

