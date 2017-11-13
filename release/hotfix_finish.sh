#!/bin/bash
set -e

SCRIPT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ $# -ne 2 ]
then
  echo 'Usage: hotfix_finish.sh <hotfix-version> <next-snapshot-version>'
  echo 'For example:'
  echo 'hotfix_finish.sh 0.2.1 0.3.0'
  exit 2
fi

HOTFIX_VERSION=$1
NEXT_VERSION=$2

CURRENT_BRANCH=`git rev-parse --abbrev-ref HEAD`

source $SCRIPT_PATH/hooks.sh

REMOTE_REPO=`get_remote_repo_name`
DEVELOP_BRANCH=`get_develop_branch_name`
MASTER_BRANCH=`get_master_branch_name`
HOTFIX_BRANCH=`format_hotfix_branch_name "$HOTFIX_VERSION"`

if [ ! "$HOTFIX_BRANCH" = "$CURRENT_BRANCH" ]
then
  echo "Please checkout the branch '$HOTFIX_BRANCH' before processing this hotfix release."
  exit 1
fi

if ! git diff-index --quiet HEAD --
then
  echo "This script is only safe when your have a clean workspace."
  echo "Please clean your workspace by stashing or commiting and pushing changes before processing this release script."
  exit 1
fi

git checkout $HOTFIX_BRANCH && git pull $REMOTE_REPO

build_snapshot_modules
git reset --hard

set_modules_version $HOTFIX_VERSION

if ! git diff-files --quiet --ignore-submodules --
then
  # commit hotfix versions
  git commit -am "Release hotfix $HOTFIX_VERSION"
else
  echo "Nothing to commit..."
fi

build_release_modules
git reset --hard

# merge current hotfix into master
git checkout $MASTER_BRANCH && git pull $REMOTE_REPO
git merge --no-edit $HOTFIX_BRANCH

# create release tag and push master
HOTFIX_TAG=`format_release_tag "$HOTFIX_VERSION"`
git tag -a "$HOTFIX_TAG" -m "Release $HOTFIX_VERSION"

git checkout $HOTFIX_BRANCH

# prepare next snapshot version
NEXT_SNAPSHOT_VERSION=`format_snapshot_version "$NEXT_VERSION"`
set_modules_version "$NEXT_SNAPSHOT_VERSION"

if ! git diff-files --quiet --ignore-submodules --
then
  # commit next snapshot versions
  git commit -am "Start next iteration with $NEXT_SNAPSHOT_VERSION after hotfix $HOTFIX_VERSION"
else
  echo "Nothing to commit..."
fi

# merge next snapshot version into develop
git checkout $DEVELOP_BRANCH

if git merge --no-edit $HOTFIX_BRANCH
then
  echo "# Okay, now you've got a new tag and commits on $MASTER_BRANCH and $DEVELOP_BRANCH"
  echo "# Please check if everything looks as expected and then push."
  echo "# Use this command to push all at once or nothing, if anything goes wrong:"
  echo "git push --atomic $REMOTE_REPO $MASTER_BRANCH $DEVELOP_BRANCH $HOTFIX_BRANCH --follow-tags # all or nothing"
else
  echo "# Okay, you have got a conflict while merging onto $DEVELOP_BRANCH"
  echo "# but don't panic, in most cases you can easily resolve the conflicts (in some cases you even do not need to merge all)."
  echo "# Please do so and continue the hotfix finishing with the following command:"
  echo "git push --atomic $REMOTE_REPO $MASTER_BRANCH $DEVELOP_BRANCH $HOTFIX_BRANCH --follow-tags # all or nothing"
fi
