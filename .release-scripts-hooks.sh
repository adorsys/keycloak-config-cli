#!/bin/bash
# ********************** INFO *********************
# This file is used to define default settings.
# Please do not change it.
# To override these settings please define functions
# with the same name in file hooks.sh in this directory
# or in file .release-script-hook.sh in parent directory
# *************************************************
set -e


# Hook method to format your release tag
# Parameter $1 - version as text
# Returns tag as text
function format_release_tag {
  echo "v$1"
}

# Hook method to format your next snapshot version
# Parameter $1 - version as text
# Returns snapshot version as text
function format_snapshot_version {
  echo "$1-SNAPSHOT"
}

# Hook method to define the remote repository name
# Returns the name of the remote repository as text
function get_remote_repo_name {
  echo "origin"
}

# Hook method to define the develop branch name
# Returns the develop branch name as text
function get_develop_branch_name {
  echo "develop"
}

# Hook method to define the master branch name
# Returns the master branch name as text
function get_master_branch_name {
  echo "master"
}

# Hook method to format the release branch name
# Parameter $1 - version as text
# Returns the formatted release branch name as text
function format_release_branch_name {
  echo "release-$1"
}

# Hook method to format the hotfix branch name
# Parameter $1 - version as text
# Returns the formatted hotfix branch name as text
function format_hotfix_branch_name {
  echo "hotfix-$1"
}

# Hook to build the snapshot modules before release
# You can build and run your tests here to avoid releasing an unstable build
function build_snapshot_modules {
  echo "do nothing" >> /dev/null
}

# Hook to build the released modules after release
# You can deploy your artifacts here
function build_release_modules {
  echo "do nothing" >> /dev/null
}

# Should set version numbers in your modules
# Parameter $1 - version as text
function set_modules_version {
  cd $SCRIPT_PATH/.. && mvn -B versions:set -DnewVersion=$1
}

# Builds the commit message used for your commit which setups the next snapshot version
# Parameter $1 - release version as text
function get_next_snapshot_commit_message {
  echo "[skip ci] Start next iteration with $1"
}

# Builds the commit message for your commit which setups the hotfix branch
# Parameter $1 - hotfix snapshot version
function get_start_hotfix_commit_message {
  echo "[skip ci] Start hotfix $1"
}

# Builds the commit message used for setup the next snapshot version after hotfix is released
# Parameter $1 - next snapshot version
# Parameter $2 - released hotfix version
function get_next_snapshot_commit_message_after_hotfix {
  echo "[skip ci] Start next iteration with $1 after hotfix $2"
}
