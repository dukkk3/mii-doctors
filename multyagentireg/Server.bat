chcp 866
echo off
cls
java -cp jade.jar;classes jade.Boot -gui -agents server1:Agents.Registry()
pause

