#!/bin/bash
set -e

SCRIPT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ $# -ne 1 ]
then
  echo 'Usage: hotfix_start.sh <hotfix-version>'
  echo 'For example:'
  echo 'hotfix_start.sh 0.2.1'
  exit 2
fi

HOTFIX_VERSION=$1
HOTFIX_SNAPSHOT_VERSION="${HOTFIX_VERSION}-SNAPSHOT"

source $SCRIPT_PATH/hooks.sh

REMOTE_REPO=`get_remote_repo_name`
DEVELOP_BRANCH=`get_develop_branch_name`
MASTER_BRANCH=`get_master_branch_name`
HOTFIX_BRANCH=`format_hotfix_branch_name "$HOTFIX_VERSION"`

if ! git diff-index --quiet HEAD --
then
  echo "This script is only safe when your have a clean workspace."
  echo "Please clean your workspace by stashing or commiting and pushing changes before processing this revert-release script."
  exit 1
fi

git checkout $MASTER_BRANCH && git pull $REMOTE_REPO
git checkout -b $HOTFIX_BRANCH

set_modules_version $HOTFIX_SNAPSHOT_VERSION

if ! git diff-files --quiet --ignore-submodules --
then
  # commit hotfix versions
  git commit -am "Start hotfix $HOTFIX_SNAPSHOT_VERSION"
else
  echo "Nothing to commit..."
fi

echo "# Okay, now you've got a new hotfix branch called $HOTFIX_BRANCH"
echo "# Please check if everything looks as expected and then push."
echo "# Use this command to push your created hotfix-branch:"
echo "git push --set-upstream $REMOTE_REPO $HOTFIX_BRANCH"
