# Scheduling Management System

Source code for the Liverpool Telescope phase2 database scheduler, which picks a suitable next observation for the robotic control system to observe.

scheduling-management-system.classpath contains an example .classpath that will allow the software to be built with eclipse (Kepler).

The compiled code ends up in the ngat_sms.jar.

The scripts/telescope.properties should be installed in occ:/occ/bin. This is read by various parts of the scheduler.

ngat.sms.bds.StartExecutionManagerAndScheduler is the entry point code to the system.

Typical command line:

java -DSCHED -Djava.rmi.server.hostname=occ.lt.com ngat.sms.bds.StartExecutionManagerAndScheduler --bind-host localhost --base-host oss.lt.com --comp-host localhost --hist-host oss.lt.com --sm-host localhost --sms-server-port 8776 --rcs-host localhost --ireg-host localhost --lat 28.7624 --long -17.8792 --log-level 4 --gls-host ltproxy --gls-port 2371 --ag-host acc.lt.com --ag-port 6571

