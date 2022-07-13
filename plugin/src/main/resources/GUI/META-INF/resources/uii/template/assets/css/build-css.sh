#!/bin/bash

SRC_BS='src/bs.scss';
DST_BS='dist/bootstrap.css'
SRC_CUSTOM='src/index.scss'
DST_CUSTOM='dist/custom.css'

# include these load paths
BS_SASS="${HOME}/projects/webapp/node_modules/bootstrap/scss/"
CUSTOM_SASS="${HOME}/projects/webapp/sass/src/"

# run sass task and include load paths (using an env var)
SASS_PATH=${BS_SASS}:${CUSTOM_SASS} sass --watch ${SRC_BS}:${DST_BS} ${SRC_CUSTOM}:${DST_CUSTOM}

