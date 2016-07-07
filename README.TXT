//
// Copyright (C) 2015  Albert Zedlitz
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

Compile the sources

> mkdir ./bin
> cp -r icons ./bin
> javac -classpath ./src -g -d ./bin
> jar   -cfm sherlok.jar MANIFEST.MF -C ./bin .

Execute the program

> java -jar sherlok.jar


To make this running with sherlok agent, specify the following line in the skp file:

--- default.skp ------
ProfileScope      = .
ProfilePackages   = com.sap.

ProfileMemory     = off
Timer             = on
Tracer            = gc

ProfileOutputType = xml
------------------------

Run the agent with a path to this file:

>  java -agentlib:sherlok=ConfigPath=<directory containing default.skp> ...

Connect with password

    port:       2424
    user:       Administrator
    password:   sherlok


See the sherlok documentation for parameter settings


