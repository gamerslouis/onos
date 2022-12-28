#!/bin/bash

installDeps() {
    echo 'Installing Dependencies...'

    sudo apt update
    sudo apt install -y build-essential bzip2 curl git maven python2 python3 unzip zip

    sudo ln -s /usr/bin/python3 /usr/bin/python
}

installBazel() {
    echo 'Installing Bazel....'

    wget https://github.com/bazelbuild/bazel/releases/download/1.2.1/bazel-1.2.1-installer-linux-x86_64.sh
    chmod +x bazel-1.2.1-installer-linux-x86_64.sh
    ./bazel-1.2.1-installer-linux-x86_64.sh --user
    export PATH="$PATH:$HOME/bin"
    echo 'export PATH="$PATH:$HOME/bin"' >> ~/.bashrc
}

installOnos() {
    echo 'Installing Onos...'

    cd ~/
    git clone https://github.com/alex94539/onos
    cd onos
    git checkout netconf-callhome-new
    bazel build onos

    export ONOS_ROOT=~/onos
    echo 'export ONOS_ROOT=~/onos' >> ~/.bashrc
    echo 'source $ONOS_ROOT/tools/dev/bash_profile' >> ~/.bashrc
}

addSshInfo() {
    echo 'Adding ssh-rsa algorithm for localhost...'

    if [[ ! -f "${HOME}/.ssh/config" ]]; then
        mkdir -p ~/.ssh
        touch ~/.ssh/config
    fi

    echo 'Host localhost
    HostkeyAlgorithms +ssh-rsa
    PubkeyAcceptedAlgorithms +ssh-rsa' >> ~/.ssh/config
}

main() {
    echo 'Welcome to onos installation script.'

    cd ~/

    installDeps
    installBazel
    installOnos

    source ~/.bashrc

    echo '*************************************'
    echo '* Environment installation finished *'
    echo '*************************************'
}

main "$@"