#!/usr/bin/env bash

# Function to determine what is set in terms of NUMA and possibly others et al
function checkHostReadiness() {
  echo "=========================================================="
  echo "Running numactl --show to determine if/how numa is enabled"
  echo 
  numactl --show
  echo "=========================================================="
}

# Get the total CPU count from the affinity.sh script
TOTAL_CPU_COUNT=0
function getTotalCPUs() {
    
    # Extract total CPU count from affinity.sh
    TOTAL_CPU_COUNT=$(get_cpu_count)

    if [ -z "$TOTAL_CPU_COUNT" ]; then
      echo "ERROR: Could not determine total CPU count, exiting"
      exit 1
    fi

    echo "CPU Count: $TOTAL_CPU_COUNT"
    export TOTAL_CPU_COUNT
}

# Make sure the O/S disks and memory et al are cleared before a run
function beforeEachRun() {
    # Call sync to force any pending disk writes. Note the user typically needs to be in the sudoers file for this to work.
    echo "============================================================="
    echo "Starting sync to flush any pending disk writes"
    sync
    echo "sync completed                                "
    echo

    # The /proc/sys/vm/drop_caches file is a special interface in the Linux kernel for managing the system's cache.
    # 3: Clear both the page cache and the dentries/inodes cache (combined effect of 1 and 2).
    # Note, the user needs permission to write to this file (we use sudo tee for this)
    echo "Clearing the memory caches                     "
    echo 3 | sudo tee /proc/sys/vm/drop_caches
    echo "Memory caches cleared                          "
    echo

    # The /sys/kernel/mm/transparent_hugepage/enabled file is a special 
    # interface in the Linux kernel for managing how users can use THP.
    # madvise: Will allow the JVM to select what to use it for (heap only).
    # Note, the user needs permission to write to this file (we use sudo tee for this)
    # TODO That cehck could be a proper check and not just catting output
    echo "Setting madvise for THP                                      "
    echo madvise | sudo tee /sys/kernel/mm/transparent_hugepage/enabled
    echo 
    echo "Checking that madvise was set:"
    echo 
    cat /sys/kernel/mm/transparent_hugepage/enabled
    echo "============================================================="

}