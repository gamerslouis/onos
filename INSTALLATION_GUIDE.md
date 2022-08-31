# INSTALLATION GUIDE

## Environment
OS: Ubuntu Server 22.04 <br>
CPU: 4Core <br>
RAM: min 8G

## INSTALLATION
```
$ sudo apt install git unzip zip build-essential python2 python3 maven -y
$ sudo ln -s /usr/bin/python3 /usr/bin/python
$ wget https://github.com/bazelbuild/bazel/releases/download/1.2.1/bazel-1.2.1-installer-linux-x86_64.sh                     #install bazel
$ chmod +x bazel-1.2.1-installer-linux-x86_64.sh
$ sudo ./bazel-1.2.1-installer-linux-x86_64     #install bazel for user
$ git clone https://github.com/alex94539/onos   #getting source code
$ cd onos && git checkout netconf-callhome-new  #switch to branch
$ bazel build onos
```

