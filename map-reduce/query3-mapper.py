#!/usr/bin/python
import sys
import os
import json
from datetime import datetime

def main(argv):
	for line in sys.stdin:
		#data_string = json.dumps(line)
		decoded = json.loads(line)
		orig_user = decoded['retweeted_status']['user']['id_str']
		current_user = decoded['user']['id_str']
		print orig_user + '\t' + current_user 

if __name__ == "__main__":
	main(sys.argv)
