import requests
import json

filepath=input("Enter the file path: ")
modified_repos = []
no_releases = []
path = "https://api.github.com/repos/"
updateflag = False

with open(filepath) as fhand:
  line = fhand.readline()
  cnt = 0
  is_latest = False

  while line:
    process_line = line.strip()

    if process_line.startswith('github_url="https://github.com/'):
      request_url = path+process_line[31:-5]+"/releases/latest"
      print(request_url)

    if process_line.startswith('tag_version'):
      req = requests.get(request_url)
      try:
        tag_version = req.json()['tag_name']
        is_latest = (tag_version == process_line[13:-1])

        # Checking whether the present tag_version is the latest one
        if(is_latest):
          print("This is the latest tag")
        else:
          line = 'tag_version="{}"\n'.format(tag_version)
          print("Tag name updated from {} to {}".format(process_line[13:-1],tag_version))
          updateflag=True

      except:
        elem = request_url+"\n"
        no_releases.append(elem)

    modified_repos.append(line)
    line = fhand.readline()
    cnt += 1
fhand.close()

# printing the urls for which latest/release donot exist
print("No latest/release for url:")
for item in no_releases:
  print(item)

# Updating the tag_version
if updateflag:
  out_file = open(filepath, "w")
  for l in modified_repos:
    out_file.write(l)
  out_file.close()

