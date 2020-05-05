#!/bin/bash -eu

# This script installs Homebrew and The R Project for Statistical Computing,
# including a number of additional R packages, on MacOS X.
# Copyright (C) 2019  KNIME GmbH
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

R_VERSION=3.6.1
INSTALL_R_DEFAULT="y"

# R packages to install
GGPLOT_2=true
DATA_TABLE=true
PMML=true
CAIRO=true
RSERVE=true

# R package repositories
REPOSITORIES='"https://cloud.r-project.org/", "http://rforge.net"'

# 'Homebrew' package manager installation flags
MAC_INSTALL_HOMEBREW=true
MAC_UNINSTALL_HOMEBREW=false

function installR {
  echo "INFO >>> Checking if R is installed ..."
  R_EXISTS=$(command -v R || echo "")

  if [[ "${R_EXISTS}" == "" ]]; then
    echo ""
    echo "INFO >>> |- No version of R was detected. Continuing with installation..."
    echo ""
    read -p "Do you want to install the R default version. Choosing 'no' allows you to specify a different version. Enter [y/n] : " INSTALL_R_DEFAULT
    if [[ "${INSTALL_R_DEFAULT}" == "n" ]]; then
      echo ""
      read -p "Enter the R version you'd like to install (Example: ${R_VERSION}) : " R_VERSION

      if [[ "${R_VERSION}" =~ ^[0-9]+.[0-9]+.[0-9]+$ ]]; then
        echo ""
        echo "INFO >>> Attempting to install R version ${R_VERSION}"
      else
        echo ""
        echo "Invalid version format. Please use the correct version format as shown in the example."
        echo ""
        exit 1
      fi
    elif [[ ! "${INSTALL_R_DEFAULT}" =~ ^[y|n]$ ]]; then
      echo ""
      echo "Unrecognized answer. Please start over."
      echo ""
      exit 1
    fi

    curl https://cran.r-project.org/bin/macosx/R-${R_VERSION}.pkg --output R-${R_VERSION}.pkg
    sudo installer -pkg ~/R-${R_VERSION}.pkg -target /
  else
    echo "INFO >>> |- R is already installed! Type 'R --version' to get more details."
  fi
}

function installPackage {
  echo "INFO >>> |- Installating required system package $1 ..."
  brew install $1
}

# !!!!!!!!!!!!!!!!!!!
# !!! - WARNING - !!!
# We need a package manager on Mac OS X to install additional system libraries.
# Installing the 'Homebrew' (https://brew.sh) package manager for that is the
# default settings.
# To disable the installation of 'Homebrew' set MAC_INSTALL_HOMEBREW=false at
# the beginning of this script.
if [[ "${MAC_INSTALL_HOMEBREW}" == "true" ]]; then
  echo "INFO >>> Checking if Homebrew package manager is installed ..."
  HOMEBREW_EXISTS=$(command -v brew || echo "")

  if [[ "${HOMEBREW_EXISTS}" == "" ]]; then
    echo "INFO >>> |- Homebrew package manager not installed. Installing Homebrew now!"
    ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
  else
    echo "INFO >>> |- Homebrew package manager is already installed! Type 'brew --version' to get more details."
  fi
fi
# !!!!!!!!!!!!!!!!!!!

# Install R if it does not already exist
installR

export R_LIBS_USER="/Users/${USER}/Library/R/${R_VERSION:0:3}/library"

# Ensure user performing the installation has proper access rights to important
# folders
sudo chown -R $(whoami) /usr/local/lib/pkgconfig /usr/local/share/info

# Install R packages if so desired. The default is TRUE for all packages. Set
# the respective variable at the beginning of the script to FALSE if you wish
# for a R package to not be installed.
if [[ "${GGPLOT_2}" == "true" ]]; then
  echo "INFO >>> Installing R package \"ggplot2\""
  echo "install.packages(\"ggplot2\", repos = c(${REPOSITORIES}))" | sudo R --no-save
fi

if [[ "${DATA_TABLE}" == "true" ]]; then
  echo "INFO >>> Installing R package \"data.table\""
  echo "install.packages(\"data.table\", repos = c(${REPOSITORIES}))" | sudo R --no-save
fi

if [[ "${PMML}" == "true" ]]; then
  echo "INFO >>> Installing R package \"pmml\""
  echo "install.packages(\"pmml\", repos = c(${REPOSITORIES}))" | sudo R --no-save
fi

if [[ "${CAIRO}" == "true" ]]; then
  echo "INFO >>> Installing R package \"Cairo\""
  echo "install.packages(\"Cairo\", repos = c(${REPOSITORIES}))" | sudo R --no-save
fi

if [[ "${RSERVE}" == "true" ]]; then
  echo "INFO >>> Installing R package \"Rserve\""

  installPackage openssl

  OPENSSL_LIB_PATH=$(brew list openssl | grep -m 1 -o '.*[1]\.[0-1]\.[0-9][a-z]\/lib\/')
  export LIBRARY_PATH=${OPENSSL_LIB_PATH}

  echo "install.packages(\"Rserve\",, \"http://rforge.net\", type=\"source\")" | sudo -E R --no-save
fi

# !!!!!!!!!!!!!!!!!!!
# !!! - WARNING - !!!
# We can now remove the previously installed 'Homebrew' (https://brew.sh)
# package manager on Mac OS X, as it is no longer needed.
# Uninstalling 'Homebrew' (https://brew.sh) package manager is not the default
# settings, i.e. by default 'Homebrew' is not uninstalled.
# If you wish to uninstall 'Homebrew' set MAC_UNINSTALL_HOMEBREW=true at the
# beginning of this script.
if [[ "${MAC_UNINSTALL_HOMEBREW}" == "true" ]]; then
  ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/uninstall)"
fi
# !!!!!!!!!!!!!!!!!!!

