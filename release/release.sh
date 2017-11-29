#!/bin/bash
set -e

SCRIPT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ -f "${SCRIPT_PATH}/.version.sh" ]; then
	source ${SCRIPT_PATH}/.version.sh
else
	VERSION="UNKNOWN VERSION"
fi

echo "Release scripts (release, version: ${VERSION})"

if [ -f "${SCRIPT_PATH}/.common-util.sh" ]; then
	source ${SCRIPT_PATH}/.common-util.sh
else
	echo 'Missing file .common-util.sh. Aborting'
	exit -1
fi

RELEASE_VERSION=$1
NEXT_VERSION=$2

if [ $# -ne 2 ]
then
  echo 'Usage: release.sh <release-version> <next-snapshot-version>'
  echo 'For example: release.sh 0.1.0 0.2.0'
  exit 2
fi

RELEASE_BRANCH=`format_release_branch_name "$RELEASE_VERSION"`

if [ ! "${CURRENT_BRANCH}" = "${DEVELOP_BRANCH}" ]
then
  echo "Please checkout the branch '${DEVELOP_BRANCH}' before processing this release script."
  exit 1
fi

check_local_workspace_state "release"

git checkout ${DEVELOP_BRANCH} && git pull ${REMOTE_REPO}
git checkout -b ${RELEASE_BRANCH}

build_snapshot_modules
git reset --hard

set_modules_version ${RELEASE_VERSION}

if ! git diff-files --quiet --ignore-submodules --
then
  # commit release versions
  git commit -am "Prepare release ${RELEASE_VERSION}"
else
  echo "Nothing to commit..."
fi

build_release_modules
git reset --hard

# merge current develop (over release branch) into master
if is_branch_existing ${MASTER_BRANCH} || is_branch_existing remotes/${REMOTE_REPO}/${MASTER_BRANCH}
then
  git checkout ${MASTER_BRANCH} && git pull ${REMOTE_REPO}
else
  git checkout -b ${MASTER_BRANCH}
  git push --set-upstream ${REMOTE_REPO} ${MASTER_BRANCH}
fi

git merge -X theirs --no-edit ${RELEASE_BRANCH}

# create release tag on master
RELEASE_TAG=`format_release_tag "${RELEASE_VERSION}"`
git tag -a "${RELEASE_TAG}" -m "Release ${RELEASE_VERSION}"

git checkout ${RELEASE_BRANCH}

NEXT_SNAPSHOT_VERSION=`format_snapshot_version "${NEXT_VERSION}"`
set_modules_version "${NEXT_SNAPSHOT_VERSION}"

if ! git diff-files --quiet --ignore-submodules --
then
  # Commit next snapshot versions into develop
  git commit -am "Start next iteration with ${NEXT_SNAPSHOT_VERSION}"
else
  echo "Nothing to commit..."
fi

git checkout ${DEVELOP_BRANCH}

if git merge --no-edit ${RELEASE_BRANCH}
then
  # Nope, doing that automtically is too dangerous. But the command is great!
  echo "# Okay, now you've got a new tag and commits on ${MASTER_BRANCH} and ${DEVELOP_BRANCH}."
  echo "# Please check if everything looks as expected and then push."
  echo "# Use this command to push all at once or nothing, if anything goes wrong:"
  echo "git push --atomic ${REMOTE_REPO} ${MASTER_BRANCH} ${DEVELOP_BRANCH} --follow-tags # all or nothing"
else
  echo "# Okay, you have got a conflict while merging onto ${DEVELOP_BRANCH}"
  echo "# but don't panic, in most cases you can easily resolve the conflicts (in some cases you even do not need to merge all)."
  echo "# Please do so and finish the release process with the following command:"
  echo "git push --atomic ${REMOTE_REPO} ${MASTER_BRANCH} ${DEVELOP_BRANCH} --follow-tags # all or nothing"
fi
