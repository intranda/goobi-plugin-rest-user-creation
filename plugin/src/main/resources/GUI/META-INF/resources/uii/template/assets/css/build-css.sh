#!/bin/bash

SRC_BS='src/bs.scss';
DST_BS='dist/bootstrap.css'
SRC_CUSTOM='src/custom.scss'
DST_CUSTOM='dist/custom.css'

# include these load paths
BS_SASS="${HOME}/git/goobi-workflow/Goobi/src/main/webapp/node_modules/bootstrap/scss/"
CUSTOM_SASS="${HOME}/git/goobi-workflow/Goobi/src/main/webapp/uii/templatePG/css/src/"

# run sass task
SASS_PATH=${BS_SASS}:${CUSTOM_SASS} sass --watch ${SRC_BS}:${DST_BS} ${SRC_CUSTOM}:${DST_CUSTOM}

