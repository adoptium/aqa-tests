#!/bin/bash

echo "Starting JCK agent.."
eval $1

pid=$!

echo "Agent started with PID $pid" 

echo "Starting JCK harness.."
eval $2

echo "Test complete. Stopping JCK Agent.." 
kill -9 $pid
