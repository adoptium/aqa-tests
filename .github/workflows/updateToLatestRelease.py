import os
import requests
import json
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('path',help="Path to the file")
args = parser.parse_args()
no_releases = []
dest_path = "https://api.github.com/repos/"
updateflag = False

files=os.listdir(args.path)

for f in files:
    if "." not in f:
        filepath = args.path+"/"+f+"/test.properties"
        if os.path.isfile(filepath):
            with open(filepath) as fhand:
                line = fhand.readline()
                cnt=0
                is_latest=False
                modified_repos = []

                while line:
                    process_line = line.strip()

                    if process_line.startswith('github_url="https://github.com/'):
                        request_url = dest_path+process_line[31:-5]+"/releases/latest"
        
                    if process_line.startswith('tag_version'):
                        req=requests.get(request_url)
                        try:
                            tag_version = req.json()['tag_name']
                            print(tag_version == process_line[13:-1])
                            is_latest = (tag_version == process_line[13:-1])

                            if(is_latest):
                                print("This is the latest tag")
                            else:
                                line = 'tag_version="{}"\n'.format(tag_version)
                                print("tag name updated from {} to {}".format(process_line[13:-1],tag_version))
                                updateflag=True

                        except:
                            elem = request_url
                            no_releases.append(elem)

                    modified_repos.append(line)   
                    line=fhand.readline()
                fhand.close()
            if updateflag:
                out_file=open(filepath,"w")
                for l in modified_repos:
                    out_file.write(l)
                out_file.close()        

# Lists all tests which donot have any release                
if no_releases:
    print("No releases for the following:")
    for item in no_releases:
        print(item)
