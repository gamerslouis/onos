#!/bin/bash

installDeps() {
    info 'Installing Dependencies...'

    sudo apt update
    sudo apt install -y build-essential bzip2 curl git maven python2 python3 unzip zip

    sudo ln -s /usr/bin/python3 /usr/bin/python
}

installBazel() {
    info 'Installing Bazel....'

    wget https://github.com/bazelbuild/bazel/releases/download/1.2.1/bazel-1.2.1-installer-linux-x86_64.sh
    chmod +x bazel-1.2.1-installer-linux-x86_64.sh
    ./bazel-1.2.1-installer-linux-x86_64.sh --user
    export PATH="$PATH:$HOME/bin"
    echo 'export PATH="$PATH:$HOME/bin"' >> ~/.bashrc
}

installOnos() {
    info 'Installing Onos...'

    cd ~
    git clone https://github.com/alex94539/onos
    cd onos
    git checkout netconf-callhome-new
    bazel build onos

    export ONOS_ROOT=~/onos
    echo 'export ONOS_ROOT=~/onos' >> ~/.bashrc
    echo 'source $ONOS_ROOT/tools/dev/bash_profile' >> ~/.bashrc
}

addSshInfo() {
    info 'Adding ssh-rsa algorithm for localhost...'

    if [[ ! -f "${HOME}/.ssh/config" ]]; then
        mkdir -p ~/.ssh
        touch ~/.ssh/config
    fi

    echo 'Host localhost
    HostkeyAlgorithms +ssh-rsa
    PubkeyAcceptedAlgorithms +ssh-rsa' >> ~/.ssh/config
}

main() {
    info 'Welcome to onos installation script.'

    installDeps
    installBazel
    installOnos

    source ~/.bashrc

    info '*************************************'
    info '* Environment installation finished *'
    info '*************************************'
}

main "$@"