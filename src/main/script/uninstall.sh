#!/bin/bash

#remove bridge jar, but not dependencies
ls -1 /mngt/bin/ |xargs -I % sh -c 'rm -v /volume/bin/ROOT/WEB-INF/lib/%'
#remove brdige configuration
ls -1 /mngt/config/ |xargs -I % sh -c 'rm -v /volume/config/%'

