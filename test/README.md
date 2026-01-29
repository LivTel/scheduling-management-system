# Schedule Management System test directory

This directory contains a test program which allows us to issue SCHEDULE_REQUESTS to the scheduler

## Building

The Makefile will build the test program TestSendScheduleRequest. You need the correct CLASSPATH for the build to work. On a development machine with access to ltdevsrv:

```code
cd ~/eclipse-workspace/scheduling-management-system/test
source sms_test_classpath
make
```

The TestSendScheduleRequest.class should be built.

## Running

You need the correct CLASSPATH to run the program. On a development machine with access to ltdevsrv, and the VPN to site up:

```code
cd ~/eclipse-workspace/scheduling-management-system/test
source sms_test_classpath
java TestSendScheduleRequest -ip occ
```

Note I am not quite sure what a successful schedule 'does', for instance does the scheduler lock a successfully scheduled group in the phase2 database so it can not be modified during an RCS exexcution of the group? There is a small chance of unintended side effects of running this program.


