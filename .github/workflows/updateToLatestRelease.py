import os
import argparse
import requests
import json

parser = argparse.ArgumentParser()
parser.add_argument('path',help="Path to the file")
args = parser.parse_args()
no_releases = []
dest_path = "https://api.github.com/repos/"
updateflag = False

files=os.listdir(args.path)
for f in files:
    filepath=args.path[-8:]+"/"+f+"/test.properties"
    print(filepath)
    with open(filepath) as fhand:
        line = fhand.readline()
        is_latest=False
        modified_repos = []

        while line:
            process_line = line.strip()

            if process_line.startswith('github_url="https://github.com/'):
                request_url = dest_path+process_line[31:-5]+"/releases/latest"
                print(request_url)

            if process_line.startswith('tag_version'):
                print(process_line)
                req=requests.get(request_url)
                print("The data for filepath "+filepath)
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
                    elem = request_url+"\n"
                    no_releases.append(elem)
                    
            modified_repos.append(line)   
            line=fhand.readline()
        fhand.close()

    if updateflag:
        out_file=open(filepath,"w")
        for l in modified_repos:
            out_file.write(l)
        out_file.close()
      
        
if no_releases:       
    print("No releases for the following:")
    for item in no_releases:
        print(item)
