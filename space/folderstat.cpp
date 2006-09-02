/* File: folderstat.cpp
 * Homepage: http://snarfed.org/space/folderstat
 * Author: Ryan Barrett (folderstat@ryanb.org)
 *
 * Uses pine to dump message headers to a file, then prints out statistics
 * about the size of the messages in each folder.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 **/


#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <numeric>
using namespace std;


/*****************************************************************************
 * CONSTANTS AND GLOBALS
 *****************************************************************************/
const string klogfile("folderstat.log");
const string kpine_tmpfile("./pine_printed.msg");
const string kdelete_cmd("rm -f ");
const string kpine_cmd("pine -I");
const string kkeys_to_folders("l,n,CR,");
const string kkeys_print_and_quit("%,i,y,q");

ofstream glog;


/*****************************************************************************
 * HELPER PROTOTYPES
 *****************************************************************************/
inline void err_exit(const string &msg, int retval = 0);
void init_log();
void pine_dump(ifstream &dump, const string &keystrokes);
void get_folder_names(vector<string> &folder_names);
int get_folder_size(int folder_num);



/*****************************************************************************
 * MAIN
 *****************************************************************************/
int main(int argc, char **argv)
{
	init_log();

	vector<string> folder_names;
	vector<int> folder_sizes;


	// print individual stats
	get_folder_names(folder_names);

	int num_folders = folder_names.size();
	for (int i = 0; i < num_folders; i++) {
		folder_sizes.push_back(get_folder_size(i));
		cout << folder_names[i] << ":\t" << folder_sizes.back() << endl;
		glog << folder_names[i] << ":\t" << folder_sizes.back() << endl;
	}


	// print aggregate stats
	int total = accumulate(folder_sizes.begin(), folder_sizes.end(), 0);
	cout << endl << "total:\t" << total << endl;
	glog << endl << "total:\t" << total << endl;

	return 0;
}



/*****************************************************************************
 * HELPER FUNCTIONS
 *****************************************************************************/

/* err_exit
 *
 * Prints the given error message and an endline, then exits with the given
 * return value.
 **/
inline void err_exit(const string &msg, int retval /* = 0 */)
{
	cout << msg << endl;
	exit(-1);
}


/* init_log
 *
 * Clears the log file and opens the glog ifstream.
 **/
void init_log()
{
	// delete tmp file if it exists
	glog.open(klogfile.c_str(), ios::trunc);
}



/* pine_dump
 *
 * Runs pine and opens the output into the dump ifstream.
 **/
void pine_dump(ifstream &dump, const string &keystrokes)
{
	// delete tmp file if it exists
	string del_cmd(kdelete_cmd + kpine_tmpfile + " &");
	system(del_cmd.c_str());


	// run pine
	string cmd(kpine_cmd + keystrokes );
	glog << "Running pine with command:" << endl << cmd << endl;
	int retval = system(cmd.c_str());

	if (retval != 0)
		err_exit("Could not write to temporary file.");


	// open tmp file
	dump.open(kpine_tmpfile.c_str());
	if (!dump.is_open() || !dump.good())
		err_exit("Could not open temporary file.");
}


/* get_folder_names
 *
 * Runs pine and writes the name of each folder to the given vector.
 **/
void get_folder_names(vector<string> &folder_names)
{
	ifstream dump;

	pine_dump(dump, kkeys_to_folders + kkeys_print_and_quit);


	// there are three lines of header before the folders; discard them
	string discard;
	getline(dump, discard);
	getline(dump, discard);
	getline(dump, discard);

	// read folder names
	string name;
	while (dump >> name)
		folder_names.push_back(name);
}





/* get_folder_size
 *
 * Runs pine, prints the specified folder (on the main mail server, second in
 * the folder list), and quits. Builds an initial keystroke list to navigate
 * pine to the specified folder, print it, and then quit.
 **/
int get_folder_size(int folder_num)
{
	// build keystroke list to go to the folder
	string cmd(kkeys_to_folders);

	for (int i = 0; i < folder_num; i++)
		cmd += "n,";
	cmd += "CR,";


	ifstream dump;
	pine_dump(dump, cmd + kkeys_print_and_quit);


	// there are three lines of header before the folders; discard them
	string discard;
	getline(dump, discard);
	getline(dump, discard);


	// add up file sizes
	int msgsize, sum = 0;
	string suffix;

	while (true) {
		getline(dump, discard, '(');
		if (!dump.good())
			break;

		dump >> msgsize;

		getline(dump, suffix, ')');
		if (suffix == "K")
			msgsize *= 1000;
		else if (suffix == "M")
			msgsize *= 1000000;

		sum += msgsize;

		getline(dump, discard);

// 		glog << "adding " << msgsize << endl;
	}

	return sum;
}

	
