#!/usr/bin/python 
import sys
import re
import happybase

def main(argv):
	current_user = None
	retweet_list = []

	connection = happybase.Connection('ec2-54-85-185-35.compute-1.amazonaws.com')
	table = connection.table('tweets')
	# input comes from STDIN
	for line in sys.stdin:
		# remove leading and trailing whitespace
		line = line.strip()

		# parse the input we got from mapper.py
		orig_user, retweet_user = line.split('\t', 1)

		if orig_user == current_user:
			retweet_list.append(retweet_user)
		else:
			if current_user:
				retweet_list.sort()
				res = ""
				for id in retweet_list:
					res = res + id + "_"
				print current_user + "," + res
				table.put(current_user,{'rt':res})

			current_user = orig_user
			retweet_list = []
			retweet_list.append(retweet_user)

	# the last one
	if current_user == orig_user:
		res = ""
		retweet_list.sort()
		for id in retweet_list:
			res = res + id + "_"
		print current_user + "," + res
		table.put(current_user,{'rt':res})

	connection.close()
		
if __name__ == "__main__":
	main(sys.argv)