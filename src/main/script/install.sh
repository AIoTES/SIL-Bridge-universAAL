#/bin/bash
#TODO check volumes are populated
cp -rv /mngt/dep/* /volume/bin/ROOT/WEB-INF/lib
cp -rv /mngt/bin/* /volume/bin/ROOT/WEB-INF/lib
cp -rv /mngt/config/* /volume/config
#TODO restart inter-mw
