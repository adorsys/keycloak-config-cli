#!/bin/bash
set -e

SCRIPT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
RELEASE_VERSION=$1
NEXT_VERSION=$2

if [ $# -ne 2 ]
then
  echo 'Usage: release.sh <release-version> <next-snapshot-version>'
  echo 'For example: release.sh 0.1.0 0.2.0'
  exit 2
fi

CURRENT_BRANCH=`git rev-parse --abbrev-ref HEAD`

source $SCRIPT_PATH/hooks.sh

REMOTE_REPO=`get_remote_repo_name`
DEVELOP_BRANCH=`get_develop_branch_name`
MASTER_BRANCH=`get_master_branch_name`
RELEASE_BRANCH=`format_release_branch_name "$RELEASE_VERSION"`

if [ ! "$CURRENT_BRANCH" = "$DEVELOP_BRANCH" ]
then
  echo "Please checkout the branch '$DEVELOP_BRANCH' before processing this release script."
  exit 1
fi

if ! git diff-index --quiet HEAD --
then
  echo "This script is only safe when your have a clean workspace."
  echo "Please clean your workspace by stashing or commiting and pushing changes before processing this release script."
  exit 1
fi

git checkout $DEVELOP_BRANCH && git pull $REMOTE_REPO
git checkout -b $RELEASE_BRANCH

build_snapshot_modules
git reset --hard

set_modules_version $RELEASE_VERSION

if ! git diff-files --quiet --ignore-submodules --
then
  # commit release versions
  git commit -am "Prepare release $RELEASE_VERSION"
else
  echo "Nothing to commit..."
fi

build_release_modules
git reset --hard

# merge current develop (over release branch) into master
git checkout $MASTER_BRANCH && git pull $REMOTE_REPO
git merge -X theirs --no-edit $RELEASE_BRANCH

# create release tag on master
RELEASE_TAG=`format_release_tag "$RELEASE_VERSION"`
git tag -a "$RELEASE_TAG" -m "Release $RELEASE_VERSION"

git checkout $RELEASE_BRANCH

NEXT_SNAPSHOT_VERSION=`format_snapshot_version "$NEXT_VERSION"`
set_modules_version "$NEXT_SNAPSHOT_VERSION"

if ! git diff-files --quiet --ignore-submodules --
then
  # Commit next snapshot versions into develop
  git commit -am "Start next iteration with $NEXT_SNAPSHOT_VERSION"
else
  echo "Nothing to commit..."
fi

git checkout $DEVELOP_BRANCH

if git merge --no-edit $RELEASE_BRANCH
then
  # Nope, doing that automtically is too dangerous. But the command is great!
  echo "# Okay, now you've got a new tag and commits on $MASTER_BRANCH and $DEVELOP_BRANCH."
  echo "# Please check if everything looks as expected and then push."
  echo "# Use this command to push all at once or nothing, if anything goes wrong:"
  echo "git push --atomic $REMOTE_REPO $MASTER_BRANCH $DEVELOP_BRANCH --follow-tags # all or nothing"
else
  echo "# Okay, you have got a conflict while merging onto $DEVELOP_BRANCH"
  echo "# but don't panic, in most cases you can easily resolve the conflicts (in some cases you even do not need to merge all)."
  echo "# Please do so and finish the release process with the following command:"
  echo "git push --atomic $REMOTE_REPO $MASTER_BRANCH $DEVELOP_BRANCH --follow-tags # all or nothing"
fi
