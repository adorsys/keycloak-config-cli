#!/bin/bash
set -e

SCRIPT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ -f "${SCRIPT_PATH}/.version.sh" ]; then
	source ${SCRIPT_PATH}/.version.sh
else
	VERSION="UNKNOWN VERSION"
fi

echo "Release scripts (hotfix-start, version: ${VERSION})"

if [ -f "${SCRIPT_PATH}/.common-util.sh" ]; then
	source ${SCRIPT_PATH}/.common-util.sh
else
	echo 'Missing file .common-util.sh. Aborting'
	exit -1
fi

if [ $# -ne 1 ]
then
  echo 'Usage: hotfix_start.sh <hotfix-version>'
  echo 'For example:'
  echo 'hotfix_start.sh 0.2.1'
  exit 2
fi

HOTFIX_VERSION=$1
HOTFIX_SNAPSHOT_VERSION="${HOTFIX_VERSION}-SNAPSHOT"

HOTFIX_BRANCH=`format_hotfix_branch_name "${HOTFIX_VERSION}"`

check_local_workspace_state "hotfix_start"

git checkout ${MASTER_BRANCH} && git pull ${REMOTE_REPO}
git checkout -b ${HOTFIX_BRANCH}

set_modules_version ${HOTFIX_SNAPSHOT_VERSION}

if ! git diff-files --quiet --ignore-submodules --
then
  # commit hotfix versions
  git commit -am "Start hotfix ${HOTFIX_SNAPSHOT_VERSION}"
else
  echo "Nothing to commit..."
fi

echo "# Okay, now you've got a new hotfix branch called ${HOTFIX_BRANCH}"
echo "# Please check if everything looks as expected and then push."
echo "# Use this command to push your created hotfix-branch:"
echo "git push --set-upstream ${REMOTE_REPO} ${HOTFIX_BRANCH}"
