<!--
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[1]https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
-->

<!-- PROJECT LOGO -->

<p align="center">
  <a href="https://adoptium.net/aqavit">
    <img src="https://adoptium.net/images/aqavit-light.png" alt="Logo" width="250">
  </a>
</p>
<br />

[![License](https://img.shields.io/github/license/Adoptium/aqa-tests)](https://github.com/adoptium/aqa-tests/blob/master/LICENSE)
[![contributors](https://img.shields.io/github/contributors/adoptium/aqa-tests)](https://github.com/adoptium/aqa-tests/graphs/contributors)
[![commit-activity](https://img.shields.io/github/commit-activity/m/adoptium/aqa-tests)](https://github.com/adoptium/aqa-tests/commits/master)
[![closed-issues](https://img.shields.io/github/issues-closed/adoptium/aqa-tests)](https://github.com/adoptium/aqa-tests/issues?q=is%3Aissue+is%3Aclosed)
[![closed-pr](https://img.shields.io/github/issues-pr-closed/adoptium/aqa-tests)](https://github.com/adoptium/aqa-tests/pulls?q=is%3Apr+is%3Aclosed)
[![release-date](https://img.shields.io/github/release-date/adoptium/aqa-tests)](https://github.com/adoptium/aqa-tests/releases)
<br />

[![OpenChain](https://img.shields.io/badge/OpenChain-white?style=for-the-badge&logo=data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHN2ZyBpZD0iTGF5ZXJfMSIgZGF0YS1uYW1lPSJMYXllciAxIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMDc0IDIxNzEuODYiPgogIDxkZWZzPgogICAgPHN0eWxlPgogICAgICAuY2xzLTEgewogICAgICAgIGZpbGw6IG5vbmU7CiAgICAgICAgc3Ryb2tlOiAjMDA4MjliOwogICAgICAgIHN0cm9rZS1taXRlcmxpbWl0OiAxMDsKICAgICAgICBzdHJva2Utd2lkdGg6IDMwcHg7CiAgICAgIH0KCiAgICAgIC5jbHMtMiB7CiAgICAgICAgZmlsbDogIzAwYWRiYjsKICAgICAgfQoKICAgICAgLmNscy0zIHsKICAgICAgICBmaWxsOiAjMDA4MjliOwogICAgICB9CgogICAgICAuY2xzLTQgewogICAgICAgIGZpbGw6ICNlMDU5MmE7CiAgICAgIH0KCiAgICAgIC5jbHMtNSB7CiAgICAgICAgZmlsbDogI2ZmZjsKICAgICAgICBmb250LWZhbWlseTogTXlyaWFkUHJvLVJlZ3VsYXIsICdNeXJpYWQgUHJvJzsKICAgICAgICBmb250LXNpemU6IDM4NHB4OwogICAgICB9CiAgICA8L3N0eWxlPgogIDwvZGVmcz4KICA8ZWxsaXBzZSBjbGFzcz0iY2xzLTEiIGN4PSIxMDM3IiBjeT0iMTAzMy44IiByeD0iMTAyMiIgcnk9IjEwMTguOCIvPgogIDxnPgogICAgPHBhdGggY2xhc3M9ImNscy00IiBkPSJtMTQ2LjY1LDEyMzguMTFjMC0xNS4wMSwyLjg4LTI5LjI1LDguNjQtNDIuNzMsNS43NS0xMy40NywxMy42Mi0yNS4yNCwyMy42LTM1LjMyLDkuOTctMTAuMDgsMjEuNjQtMTguMDUsMzUuMDEtMjMuOTEsMTMuMzctNS44NiwyNy42Ni04Ljc5LDQyLjg4LTguNzlzMjkuMjUsMi45Myw0Mi43Miw4Ljc5YzEzLjQ3LDUuODYsMjUuMjQsMTMuODMsMzUuMzIsMjMuOTEsMTAuMDcsMTAuMDgsMTguMDUsMjEuODUsMjMuOTEsMzUuMzIsNS44NiwxMy40Nyw4Ljc5LDI3LjcyLDguNzksNDIuNzNzLTIuOTMsMjkuNTEtOC43OSw0Mi44OGMtNS44NiwxMy4zNy0xMy44MywyNS4wNC0yMy45MSwzNS4wMS0xMC4wOCw5Ljk4LTIxLjg1LDE3Ljg0LTM1LjMyLDIzLjYtMTMuNDcsNS43Ni0yNy43MSw4LjY0LTQyLjcyLDguNjRzLTI5LjUxLTIuODgtNDIuODgtOC42NGMtMTMuMzctNS43NS0yNS4wNC0xMy42Mi0zNS4wMS0yMy42LTkuOTgtOS45Ny0xNy44NC0yMS42NC0yMy42LTM1LjAxLTUuNzYtMTMuMzYtOC42NC0yNy42Ni04LjY0LTQyLjg4Wm0yNS42LDBjMCwxMS45MywyLjIxLDIzLjA5LDYuNjMsMzMuNDcsNC40MiwxMC4zOSwxMC40OSwxOS40MywxOC4yLDI3LjE1LDcuNzEsNy43MSwxNi43MSwxMy44MywyNi45OSwxOC4zNSwxMC4yOCw0LjUzLDIxLjI4LDYuNzksMzMuMDEsNi43OXMyMi43Mi0yLjI2LDMzLjAxLTYuNzljMTAuMjgtNC41MiwxOS4yOC0xMC42NCwyNi45OS0xOC4zNSw3LjcxLTcuNzEsMTMuNzgtMTYuNzYsMTguMi0yNy4xNSw0LjQyLTEwLjM4LDYuNjMtMjEuNTQsNi42My0zMy40N3MtMi4yMS0yMy4wOC02LjYzLTMzLjQ3Yy00LjQyLTEwLjM4LTEwLjQ5LTE5LjQ4LTE4LjItMjcuMy03LjcxLTcuODEtMTYuNzEtMTMuOTgtMjYuOTktMTguNTEtMTAuMjktNC41Mi0yMS4yOC02Ljc5LTMzLjAxLTYuNzlzLTIyLjcyLDIuMjctMzMuMDEsNi43OWMtMTAuMjksNC41My0xOS4yOCwxMC43LTI2Ljk5LDE4LjUxLTcuNzEsNy44Mi0xMy43OCwxNi45Mi0xOC4yLDI3LjMtNC40MiwxMC4zOS02LjYzLDIxLjU0LTYuNjMsMzMuNDdaIi8+CiAgICA8cGF0aCBjbGFzcz0iY2xzLTQiIGQ9Im00MDQuNTMsMTM0NS43NnYtMjE1LjkzaDUwLjljNi45OSwwLDE0Ljc1LDEuMDgsMjMuMjksMy4yNCw4LjUzLDIuMTYsMTYuNSw1LjYxLDIzLjkxLDEwLjMzLDcuNCw0LjczLDEzLjU3LDEwLjgsMTguNTEsMTguMnM3LjQsMTYuMzUsNy40LDI2Ljg0YzAsMTguNzItNS41NSwzMy41OC0xNi42Niw0NC41Ny0xMS4xMSwxMS0yNy4xNSwxNi41LTQ4LjEyLDE2LjVoLTMzLjYydjk2LjI0aC0yNS42Wm0yNS42LTEyMC45MmgyNi44NGM5LjA1LDAsMTYuNS0xLjE4LDIyLjM2LTMuNTUsNS44Ni0yLjM2LDEwLjU0LTUuMjksMTQuMDQtOC43OSwzLjQ5LTMuNDksNS45Ni03LjQsNy40LTExLjcyLDEuNDQtNC4zMiwyLjE2LTguNDMsMi4xNi0xMi4zNCwwLTQuNTItMS4xOS04Ljg5LTMuNTUtMTMuMTEtMi4zNy00LjIxLTUuNTUtNy45MS05LjU2LTExLjEtNC4wMS0zLjE5LTguODQtNS43MS0xNC41LTcuNTYtNS42Ni0xLjg1LTExLjY3LTIuNzctMTguMDQtMi43N2gtMjcuMTV2NzAuOTVaIi8+CiAgICA8cGF0aCBjbGFzcz0iY2xzLTQiIGQ9Im01NTguNzcsMTM0NS43NnYtMjE1LjkzaDEzMi45NXYyNC4zN2gtMTA3LjM1djcwLjY0aDk1LjMydjI0LjM3aC05NS4zMnY3Mi4xOGgxMTEuNjd2MjQuMzdoLTEzNy4yN1oiLz4KICAgIDxwYXRoIGNsYXNzPSJjbHMtNCIgZD0ibTk0Mi4xOSwxMTI5LjgzdjIyNS44Yy0yNi41My0yOC4xNy01Mi44NS01Ni4xNC03OC45Ny04My45LTI2LjEyLTI3Ljc2LTUyLjQ0LTU1LjczLTc4Ljk3LTgzLjktMS4wMy0xLjQ0LTItMi43OC0yLjkzLTQuMDEtLjkzLTEuMjMtMS45LTIuNTctMi45My00LjAxLjIsMS42NS4zNiwzLjI5LjQ2LDQuOTMuMSwxLjY1LjI2LDMuMjkuNDYsNC45NHYxNTYuMDhoLTI1LjZ2LTIyNS44YzI2LjUzLDI4Ljc5LDUyLjg1LDU3LjMzLDc4Ljk3LDg1LjYsMjYuMTEsMjguMjgsNTIuNDQsNTYuODEsNzguOTcsODUuNiwxLjAzLDEuNDQsMi4wMSwyLjc4LDIuOTMsNC4wMS45MywxLjIzLDEuOSwyLjU3LDIuOTMsNC4wMS0uMjEtMS42NC0uMzYtMy4yOS0uNDYtNC45NC0uMTEtMS42NC0uMjYtMy4yOS0uNDYtNC45M3YtMTU5LjQ4aDI1LjZaIi8+CiAgICA8cGF0aCBjbGFzcz0iY2xzLTIiIGQ9Im0xMTQ4LjU2LDEzMTcuMzhsMTAuNDksMTUuNDJjLTguMjMsNC43My0xNy4wMiw4LjQ4LTI2LjM4LDExLjI2LTkuMzYsMi43Ny0xOS4yOCw0LjE2LTI5Ljc3LDQuMTYtMTUuMjIsMC0yOS41MS0yLjg4LTQyLjg4LTguNjQtMTMuMzctNS43NS0yNS4wNC0xMy42Mi0zNS4wMS0yMy42LTkuOTgtOS45Ny0xNy44NC0yMS42NC0yMy42LTM1LjAxLTUuNzYtMTMuMzYtOC42NC0yNy42Ni04LjY0LTQyLjg4czIuODgtMjkuNTYsOC42NC00My4wM2M1Ljc1LTEzLjQ3LDEzLjYyLTI1LjE5LDIzLjYtMzUuMTcsOS45Ny05Ljk3LDIxLjY0LTE3Ljg5LDM1LjAxLTIzLjc1LDEzLjM3LTUuODYsMjcuNjYtOC43OSw0Mi44OC04Ljc5LDEwLjQ5LDAsMjAuNDEsMS4zNCwyOS43Nyw0LjAxLDkuMzYsMi42NywxOC4yNSw2LjQ4LDI2LjY4LDExLjQxLTEuNjUsMi42OC0zLjM5LDUuMy01LjI0LDcuODctMS44NSwyLjU3LTMuNyw1LjItNS41NSw3Ljg2LTEzLjc4LTguNDMtMjktMTIuNjUtNDUuNjYtMTIuNjUtMTIuNTQsMC0yNC4zMiwyLjQyLTM1LjMyLDcuMjUtMTEsNC44My0yMC42MiwxMS40Ny0yOC44NCwxOS45LTguMjMsOC40NC0xNC43NiwxOC4yNS0xOS41OSwyOS40Ni00LjgzLDExLjIxLTcuMjUsMjMuMDktNy4yNSwzNS42M3MyLjM2LDI0LjY4LDcuMDksMzUuNzhjNC43MywxMS4xMSwxMS4yMSwyMC44MiwxOS40MywyOS4xNSw4LjIyLDguMzMsMTcuODQsMTQuOTEsMjguODQsMTkuNzQsMTEsNC44MywyMi43Nyw3LjI1LDM1LjMyLDcuMjUsOC40MywwLDE2LjQ1LTEuMDgsMjQuMDYtMy4yNCw3LjYxLTIuMTYsMTQuOTEtNS4yOSwyMS45LTkuNDFaIi8+CiAgICA8cGF0aCBjbGFzcz0iY2xzLTIiIGQ9Im0xMjA2Ljg2LDEzNDUuNzZ2LTIxNS45M2gxOS4xMnY5OC4wOWgxMjQuNjJ2LTk4LjA5aDE5LjQzYzAsMzYuMi0uMDUsNzIuMTgtLjE1LDEwNy45Ny0uMTEsMzUuNzgtLjE1LDcxLjc3LS4xNSwxMDcuOTZoLTE5LjEzdi05OS42NGgtMTI0LjYydjk5LjY0aC0xOS4xMloiLz4KICAgIDxwYXRoIGNsYXNzPSJjbHMtMiIgZD0ibTE2MDYuMzIsMTM0NS43NmgtMTkuNDRjLTYuMzctMTQuODEtMTIuNjUtMjkuNjYtMTguODItNDQuNTctNi4xNy0xNC45MS0xMi40NC0yOS43Ny0xOC44Mi00NC41N2gtNzcuNDNjLTYuMTcsMTQuODEtMTIuMzksMjkuNjctMTguNjYsNDQuNTctNi4yNywxNC45MS0xMi42LDI5Ljc3LTE4Ljk3LDQ0LjU3aC0xOS4xMmMxNi4wNC0zNy44MywzMS45OC03NS40Nyw0Ny44MS0xMTIuOSwxNS44My0zNy40MywzMS43Ny03NS4wNiw0Ny44MS0xMTIuOSwxNi4wNCwzNy44NCwzMS45OCw3NS40Nyw0Ny44MSwxMTIuOSwxNS44NCwzNy40MywzMS43Nyw3NS4wNyw0Ny44MiwxMTIuOVptLTkyLjg1LTE3My42N2MtLjQxLTEuNjQtLjg4LTMuMjQtMS4zOS00Ljc4LS41Mi0xLjU0LS45OC0zLjEzLTEuMzktNC43OC00LjExLDEyLjk2LTguOTQsMjUuNzEtMTQuNSwzOC4yNS01LjU1LDEyLjU1LTExLDI1LjA5LTE2LjM1LDM3LjYzaDYxLjM5Yy00LjczLTExLjExLTkuMzYtMjIuMTYtMTMuODgtMzMuMTYtNC41My0xMS05LjE1LTIyLjA1LTEzLjg4LTMzLjE2WiIvPgogICAgPHBhdGggY2xhc3M9ImNscy0yIiBkPSJtMTY1NS42NywxMTI5LjgzaDE5LjQ0djIxNS45M2gtMTkuNDR2LTIxNS45M1oiLz4KICAgIDxwYXRoIGNsYXNzPSJjbHMtMiIgZD0ibTE5MjQuOTcsMTEyOS44M3YyMjUuOGMtMjcuNTYtMzAuNjQtNTQuOTYtNjEuMDMtODIuMjEtOTEuMTYtMjcuMjUtMzAuMTItNTQuNjUtNjAuNTEtODIuMjEtOTEuMTUtMS4wMy0xLjQ0LTItMi43OC0yLjkzLTQuMDEtLjkzLTEuMjMtMS45LTIuNTctMi45My00LjAxLjIsMS42NS4zNiwzLjI5LjQ2LDQuOTMuMSwxLjY1LjI2LDMuMjkuNDYsNC45NHYxNzAuNThoLTE5LjEydi0yMjUuOGMyNy41NSwzMS4wNSw1NC45NSw2MS45LDgyLjIxLDkyLjU0LDI3LjI1LDMwLjY1LDU0LjY1LDYxLjQ5LDgyLjIxLDkyLjU0LDEuMDMsMS40NCwyLDIuNzgsMi45Myw0LjAxLjkzLDEuMjMsMS45LDIuNTcsMi45Myw0LjAxLS4yMS0xLjY0LS4zNi0zLjI5LS40Ni00Ljk0LS4xMS0xLjY0LS4yNi0zLjI5LS40Ni00Ljkzdi0xNzMuMzZoMTkuMTJaIi8+CiAgPC9nPgogIDxnPgogICAgPGc+CiAgICAgIDxwYXRoIGNsYXNzPSJjbHMtNCIgZD0ibTg3NS4wOCw4NDIuNjhjLTIuMTQsMC00LjI3LS44OS01LjgtMi42Mi0yLjgyLTMuMi0yLjUxLTguMDkuNjktMTAuOTEsNTQuNzEtNDguMiw4Ni4wOS0xMTcuNjIsODYuMDktMTkwLjQ2LDAtNDQuNi0xMS43NC04OC40OC0zMy45Ni0xMjYuODgtMi4xNC0zLjY5LS44OC04LjQyLDIuODItMTAuNTYsMy43LTIuMTQsOC40Mi0uODgsMTAuNTYsMi44MiwyMy41OCw0MC43NSwzNi4wNCw4Ny4zMSwzNi4wNCwxMzQuNjMsMCw3Ny4yOC0zMy4yOSwxNTAuOTMtOTEuMzMsMjAyLjA2LTEuNDcsMS4yOS0zLjI5LDEuOTMtNS4xLDEuOTNaIi8+CiAgICAgIDxwYXRoIGNsYXNzPSJjbHMtNCIgZD0ibTcwMi4zMSw5MDcuODljLTE0OC40MywwLTI2OS4yLTEyMC43Ni0yNjkuMi0yNjkuMnMxMjAuNzYtMjY5LjE5LDI2OS4yLTI2OS4xOWMzMC4wNywwLDU5LjU3LDQuOTEsODcuNzEsMTQuNiw0LjAzLDEuMzksNi4xOCw1Ljc4LDQuNzksOS44Mi0xLjM5LDQuMDMtNS44LDYuMTctOS44Miw0Ljc5LTI2LjUxLTkuMTMtNTQuMzMtMTMuNzYtODIuNjgtMTMuNzYtMTM5LjkyLDAtMjUzLjc0LDExMy44My0yNTMuNzQsMjUzLjczczExMy44MywyNTMuNzUsMjUzLjc0LDI1My43NWM0LjI3LDAsNy43MywzLjQ2LDcuNzMsNy43M3MtMy40Niw3LjczLTcuNzMsNy43M1oiLz4KICAgIDwvZz4KICAgIDxnPgogICAgICA8cGF0aCBjbGFzcz0iY2xzLTIiIGQ9Im0xMzgxLjYzLDkwNy44OWMtMTI3LjMzLDAtMjM4LjI0LTkwLjQtMjYzLjcyLTIxNC45NS0uODYtNC4xOCwxLjg0LTguMjcsNi4wMi05LjEyLDQuMTctLjg5LDguMjYsMS44NCw5LjEyLDYuMDIsMjQuMDMsMTE3LjQsMTI4LjU2LDIwMi42LDI0OC41OCwyMDIuNiw0LjI3LDAsNy43MywzLjQ2LDcuNzMsNy43M3MtMy40Niw3LjczLTcuNzMsNy43M1oiLz4KICAgICAgPHBhdGggY2xhc3M9ImNscy0yIiBkPSJtMTU1NC41OCw4NDIuNTNjLTIuMTQsMC00LjI2LS44OC01LjgtMi42MS0yLjgyLTMuMi0yLjUyLTguMDguNjgtMTAuOTEsNTQuNi00OC4xOCw4NS45MS0xMTcuNTUsODUuOTEtMTkwLjMyLDAtMTM5LjkxLTExMy44My0yNTMuNzMtMjUzLjc1LTI1My43My05MC4zMSwwLTE3NC41Miw0OC42MS0yMTkuNzksMTI2Ljg1LTIuMTMsMy42OS02Ljg2LDQuOTUtMTAuNTYsMi44Mi0zLjY5LTIuMTQtNC45Ni02Ljg3LTIuODEtMTAuNTYsNDguMDEtODMsMTM3LjM2LTEzNC41NiwyMzMuMTYtMTM0LjU2LDE0OC40NCwwLDI2OS4yMSwxMjAuNzYsMjY5LjIxLDI2OS4xOSwwLDc3LjItMzMuMjIsMTUwLjc4LTkxLjE0LDIwMS45MS0xLjQ3LDEuMjktMy4zLDEuOTMtNS4xMiwxLjkzWiIvPgogICAgPC9nPgogICAgPGc+CiAgICAgIDxwYXRoIGNsYXNzPSJjbHMtMyIgZD0ibTEyNTYuMzMsNDgyLjM3cy0uMDcuMDYtLjExLjA5Yy0yLjQ1LDEuOTctMi44Niw1LjQ5LTEuMDgsOC4wNywyOC40Niw0MS4yNyw0NS4xOCw5MS4yMyw0NS4xOCwxNDUuMDVzLTE3Ljc0LDEwNi45NC00Ny44NSwxNDguOTRjLTEuODMsMi41Ni0xLjQ4LDYuMDkuOTUsOC4xLjAzLjAzLjA2LjA1LjA5LjA4LDIuNzEsMi4yNCw2Ljc4LDEuODIsOC44My0xLjA0LDMxLjU2LTQ0LjAxLDUwLjE3LTk3LjkxLDUwLjE3LTE1Ni4wOHMtMTcuNTMtMTA4Ljc2LTQ3LjM3LTE1Mi4wMmMtMi0yLjktNi4wNy0zLjM5LTguODItMS4xOVoiLz4KICAgICAgPHBhdGggY2xhc3M9ImNscy0zIiBkPSJtMTE1Ny4xNiw4NjkuNzljLTEuNzktMS43My00LjQyLTIuMTgtNi42OC0xLjE0LTMyLjQzLDE0Ljg3LTY4LjQ3LDIzLjE4LTEwNi40MiwyMy4xOHMtNzUuOS04Ljc3LTEwOS0yNC40Yy0yLjI4LTEuMDgtNC45Ni0uNjMtNi43NiwxLjE0LS4wMy4wMy0uMDYuMDYtLjA5LjA5LTIuOTcsMi45LTIuMiw3Ljk0LDEuNTYsOS43MiwzNC43LDE2LjQxLDczLjQzLDI1LjY1LDExNC4yOSwyNS42NXM3Ny41OS04Ljc2LDExMS42LTI0LjM4YzMuODEtMS43NSw0LjYxLTYuODUsMS41OS05Ljc3LS4wMy0uMDMtLjA2LS4wNi0uMDktLjA5WiIvPgogICAgPC9nPgogICAgPHBhdGggY2xhc3M9ImNscy0zIiBkPSJtODM3LjMxLDc5NC44NGMtMi4yNSwwLTQuNDgtLjk4LTYuMDEtMi44Ni0xLjI3LTEuNTctMTI1LjM2LTE1OC4zOC01LjU5LTMxNS42NiwxMzQuMTctMTc2LjIsMzM0LjY3LTg0LjQzLDMzNi42OS04My40OCwzLjg2LDEuODIsNS41MSw2LjQyLDMuNjksMTAuMjgtMS44MSwzLjg2LTYuNDIsNS41MS0xMC4yOCwzLjctNy43NS0zLjY0LTE5MS4zLTg3LjI2LTMxNy44MSw3OC44Ni0xMTIuNSwxNDcuNzMsNC4xMiwyOTUuMSw1LjMxLDI5Ni41NywyLjY4LDMuMzIsMi4xNyw4LjE4LTEuMTQsMTAuODctMS40MywxLjE2LTMuMTUsMS43Mi00Ljg2LDEuNzJaIi8+CiAgPC9nPgogIDxlbGxpcHNlIGNsYXNzPSJjbHMtMyIgY3g9IjEwMzguNiIgY3k9IjE4NDUuODQiIHJ4PSIzMjcuMDQiIHJ5PSIzMjYuMDIiLz4KICA8dGV4dCBjbGFzcz0iY2xzLTUiIHRyYW5zZm9ybT0idHJhbnNsYXRlKDc5NS40NyAxOTc3LjczKSI+PHRzcGFuIHg9IjAiIHk9IjAiPjIuMDwvdHNwYW4+PC90ZXh0Pgo8L3N2Zz4=)](https://www.eclipse.org/openchain/)
[![slack](https://img.shields.io/badge/Slack-4A154B?logo=slack&logoColor=white)](https://adoptium.net/slack/)
[![Twitter](https://img.shields.io/twitter/follow/adoptium?style=social)](https://twitter.com/adoptium)

# Adoptium Testing

#### Guide to the Test Jobs at Adoptium

For nightly and release builds, there are test jobs running as part of the Adoptium continuous delivery pipelines.  There is a [blog post and brief presentation](https://blog.adoptopenjdk.net/2017/12/testing-java-help-count-ways) that explains what testing we run and how they fit into the overall delivery pipeline.  As the world of testing at Adoptium is evolving and improving quickly, some documentation may fall behind the march of progress.  Please let us know and help us keep it up-to-date, and ask questions at the [Adoptium testing Slack channel](https://adoptium.slack.com/archives/C5219G28G)!

![CI pipeline view](doc/diagrams/ciPipeline.jpg)

#### Test 'Inventory'

The directory structure in this aqa-tests repository is meant to reflect the different types of test we run (and pull from lots of other locations).  The diagrams below show the test make target for each of the types, along with in-plan, upcoming additions (denoted by dotted line grey boxes). The provided links jump to test jobs in Jenkins (ci.adoptium.net).

```mermaid
graph TD
    A[openjdk-tests] -->B[make _perf]
    A[openjdk-tests] -->C[make _openjdk]
    A[openjdk-tests] -->D[make _system]
    A[openjdk-tests] -->E[make _functional]
    A[openjdk-tests] -->F[make _jck]
    A[openjdk-tests] -->G[make _external]
    B[make _perf] -->|perf|H[performance]
    H[performance] -->|_sanity.perf|I[.....]
    H[performance] -->|_extended.perf|J[..]
    C[make _openjdk] -->|openjdk|K[openjdk]
    D[make _system] -->|system|L[system]
    E[make _functional] -->|functional|M[functional]
    F[make _jck] -->|jck|N[jck]
    G[make _external] -->|external|O[external]
    O[external] -->|_sanity.external|P[...]
    O[external] -->|_extended.external|Q[....] 
    
```

--- 

##### [openjdk](https://ci.adoptium.net/view/Test_openjdk/) tests - OpenJDK regression tests 
Tests from OpenJDK

--- 

##### [system](https://ci.adoptium.net/view/Test_system/) tests - System and load tests 
Tests from the adoptium/aqa-systemtest repo

--- 

##### [external](https://ci.adoptium.net/view/Test_external/) tests - 3rd party application tests
Test suites from a variety of applications, along with microprofile TCKs, run in Docker containers

```mermaid
graph TD
 A[openjdk-tests] -->|make _external| B[external]
    B --> C[derby]
    B --> D[elasticsearch]
    B --> E[example]
    B --> F[jenkins]
    B --> G[kafka]
    B --> H[lucene-solr]
    B -->|_sanity.external|I[scala]
    B --> J[tomcat]
    B --> K[wildfly]
    B --> L[openliberty]
    B --> M[geode]
    B --> N[hbase]
    B --> O[akka]
    B --> P[logstash]
    B --> Q[openliberty-mp-tck]
    B -->|_extended.external|R[payara-mp-tck]
    B --> S[thorntail-mp-tck]
   
```

--- 

##### [perf](https://ci.adoptium.net/view/Test_perf/) tests - Performance benchmark suites 
Performance benchmark tests (both full suites and microbenches) from different open-source projects such as Acme-Air and adoptium/bumblebench

```mermaid
graph TD
 A[openjdk-tests] -->|make _perf| B[performance]
    B -->|_sanity.perf| C[bbench]
    B --> D[idle_micro]
    B --> E[odm]
    B -->|_extended.perf| F[liberty_dt]
    B --> G[acme_air]
   ```

--- 

##### [functional](https://ci.adoptium.net/view/Test_functional/) tests - Unit and functional tests
Functional tests not originating from the openjdk regression suite, that include locale/language tests and a subset of implementation agnostic tests from the openj9 project.

--- 

##### jck tests - Compliance tests
TCK tests (under the OpenJDK Community TCK License Agreement), in compliance with the license agreement.  While this test material is currently not run at the Adoptium project (see the [support statement](https://adoptopenjdk.net/support.html#jck) for details), those with their own OCTLA agreements may use the Adoptium test automation infrastructure to execute their TCK test material in their own private Jenkins servers.

--- 

#### Guide to Running the Tests Yourself
For more details on how to run the same tests that we run at Adoptium on your laptop or in your build farm, please consult our [User Guide](doc/userGuide.md) (work in progress).

#### What is our motivation?
We want:
- better, more flexible tests, with the ability to apply certain types of testing to different builds
- a common way to easily add, edit, group, include, exclude and execute tests on adoptium builds
- the latitude to use a variety of tests that use many different test frameworks
- test results to have a common look & feel for easier viewing and comparison

There are a great number of tests available to test a JVM, starting with the OpenJDK regression tests.  In addition to running the OpenJDK regression tests, we will increase the amount of testing and coverage by pulling in other open tests.  These new tests are not necessarily written using the jtreg format.

Why the need for other testing?  The OpenJDK regression tests are a great start, but eventually you may want to be able to test how performant is your code, and whether some 3rd party applications still work.  We will begin to incorporate more types of testing, including:
- additional API and functional tests
- stress/load tests
- system level tests such as 3rd party application tests
- performance tests
- TCK tests

The test infrastructure in this repository allows us to lightly yoke a great variety of tests together to be applied to testing the adoptium binaries.  By using an intentionally thin wrapper around a varied set of tests, we can more easily run all types of tests via make targets and as stages in our Jenkins CI pipeline builds.


#### How can you help?
You can:
- browse through the [aqa-tests issues list](https://github.com/adoptium/aqa-tests/issues), select one, add a comment to claim it and ask questions
- browse through the [aqa-systemtest issues](https://github.com/adoptium/aqa-systemtest/issues) or [stf issues](https://github.com/adoptium/stf/issues), claim one with a comment and dig right in
- triage live test jobs at [ci.adoptium.net](https://ci.adoptium.net), check out the [triage doc](https://github.com/adoptium/aqa-tests/blob/master/doc/Triage.md) for guidance
  - if you would like to regularly triage test jobs, you can optionally 'sign up for duty' via the [triage rotas](https://github.com/adoptium/aqa-tests/wiki/AdoptOpenJDK-Test-Triage-Rotas)
- ask questions in the [#testing channel](https://adoptium.slack.com/archives/C5219G28G) 
