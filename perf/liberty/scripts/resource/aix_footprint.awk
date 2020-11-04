#!/bin/awk -f

################################################################################
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
################################################################################

#------------------------------------------------------------------------------
# An AWK script to parse the output of svmon, selectively ignoring the client
# segments which correspond to file caches (ie. which are not mmap'd into the
# process address space).
# This works with both Command and Pid reports.
# You MUST specify the following flags for your svmon report:
# -O segment=category,unit=KB,mapping=on,format=80
#  You can use unit=MB if you prefer (slightly less accurate)
#  You can use pgsz=on (but the PageSize information is ignored by this parser)
#
# Set VERBOSE=1 to get progress / debugging type messages (might help if things
# go wrong)
# Set ECHO=1 to see the original lines of svmon output echoed during processing
# (useful with VERBOSE, or to verify the sanity of the parsed figures)
#------------------------------------------------------------------------------

BEGIN {
  VERBOSE = 0
  ECHO = 1
  state = "START"
  if ( VERBOSE ) print "AWK-based footprint parser for svmon"
}

{ if ( ECHO ) print $0 }
{ line ++ }

# Change state when significant lines are reached in the svmon report
/Command/ { state = "COMMAND" }
/SYSTEM/ { state = "SYSTEM" }
/EXCLUSIVE/ { state = "EXCLUSIVE" }
/SHARED/ { state = "SHARED" }
/Vsid.*Esid/ { state = "SEGMENT_LIST" }

# We might find a PROCMAP report before or after the svmon - these are the
# start and end states of that report.
/[0-9]+ : .*java/ { state = "PROCMAP_START" }
/ +Total +[0-9]+K/ { state = "PROCMAP_TOTAL" }

# Extracts headings for the command summary.
# ---------------------------Example-input-line--------------------------------
# Command                              Inuse      Pin     Pgsp  Virtual
# -----------------------------------------------------------------------------
# Leaves results in an array: headings
#
state == "COMMAND" {
  split( $0, headings )
  if ( /Pid/ ) {
    if ( VERBOSE ) print "Line", line ":", "This appears to be a PID report"
    pid_report=1
  } else {
    if ( VERBOSE ) print "Line", line ":", "This appears to be a Command report"
  }
  state = "COMMAND_SUMMARY"
  next
}

# Collects command summary values.
# ---------------------------Example-input-line--------------------------------
# java                                        361796    29244        0   284984
# -----------------------------------------------------------------------------
# Leaves results in variables: command_summary_inuse, command_summary_virtual,
#    command, pid  (pid only if the report being processed is a Pid report)
#
state == "COMMAND_SUMMARY" {
    split( $0, data )
    for ( idx in data ) {
      values[headings[idx]] = data[idx]
    }
    pid = values["Pid"]
    command = values["Command"]
    command_summary_inuse = values["Inuse"]
    command_summary_virtual = values["Virtual"]
	state = "SEARCH"
}

# Extracts headings for the SYSTEM summary.
# ---------------------------Example-input-line--------------------------------
# SYSTEM segments                      Inuse      Pin     Pgsp  Virtual
# -----------------------------------------------------------------------------
# Leaves results in an array: headings
#
state == "SYSTEM" {
  if ( VERBOSE ) print "Line", line ":", "Parsing SYSTEM headers"
  head_offset = index( $0, "segments" ) + 8
  split( substr( $0, head_offset ), headings )
  state = "SYSTEM_SUMMARY"
  next
}

# Extracts Inuse and Virtual values from the SYSTEM summary
# ---------------------------Example-input-line--------------------------------
#                                      32424    29228        0    32424
# -----------------------------------------------------------------------------
# Leaves results in variables: system_summary_inuse, system_summary_virtual
#
state == "SYSTEM_SUMMARY" {
  split( $0, data )
    for ( idx in data ) {
      values[headings[idx]] = data[idx]
    }
	system_summary_inuse = values["Inuse"]
	system_summary_virtual = values["Virtual"]
	state = "SEARCH"
	next_state = "SYSTEM_LIST"
}

# Extracts headings for the EXCLUSIVE summary
# ---------------------------Example-input-line--------------------------------
# EXCLUSIVE segments                   Inuse      Pin     Pgsp  Virtual
# -----------------------------------------------------------------------------
# Leaves results in an array: headings
#
state == "EXCLUSIVE" {
  if ( VERBOSE ) print "Line", line ":", "Parsing EXCLUSIVE headers"
  head_offset = index( $0, "segments" ) + 8
  split( substr( $0, head_offset ), headings )
  state = "EXCLUSIVE_SUMMARY"
  next
}

# Extracts Inuse and Virtual values from the EXCLUSIVE summary
# ---------------------------Example-input-line--------------------------------
#                                     247072       16        0   170268
# -----------------------------------------------------------------------------
# Leaves results in variables: exclusive_summary_inuse, exclusive_summary_virtual
#
state == "EXCLUSIVE_SUMMARY" {
  split( $0, data )
    for ( idx in data ) {
      values[headings[idx]] = data[idx]
    }
	exclusive_summary_inuse = values["Inuse"]
	exclusive_summary_virtual = values["Virtual"]
	state = "SEARCH"
	next_state = "EXCLUSIVE_LIST"
}

# Extracts headings for the SHARED summary
# ---------------------------Example-input-line--------------------------------
# SHARED segments                      Inuse      Pin     Pgsp  Virtual
# -----------------------------------------------------------------------------
# Leaves results in an array: headings
#
state == "SHARED" {
  if ( VERBOSE ) print "Line", line ":", "Parsing SHARED headers"
  head_offset = index( $0, "segments" ) + 8
  split( substr( $0, head_offset ), headings )
  state = "SHARED_SUMMARY"
  next
} 

# Extracts Inuse and Virtual values from the SHARED summary
# ---------------------------Example-input-line--------------------------------
#                                      82300        0        0    82292
# -----------------------------------------------------------------------------
# Leaves results in variables: shared_summary_inuse, shared_summary_virtual
#
state == "SHARED_SUMMARY" {
  split( $0, data )
    for ( idx in data ) {
      values[headings[idx]] = data[idx]
    }
	shared_summary_inuse = values["Inuse"]
	shared_summary_virtual = values["Virtual"]
	state = "SEARCH"
	next_state = "SHARED_LIST"
}

# Grabs the headings for a segment listing.
# ---------------------------Example-input-line--------------------------------
#     Vsid      Esid Type Description           PSize  Inuse   Pin Pgsp Virtual
# -----------------------------------------------------------------------------
# Leaves results in variables:
# - headings      - a list of headings, numeric indexed
# - head_count    - the number of headings
#
state == "SEGMENT_LIST" {
  if ( VERBOSE ) print "Line", line ":", "Start of segment list"
  head_count = split( $0, headings )
  state = "SEGMENT_LIST_ENTRIES"
  next
}

# This code is going to step through the 'fragments' of values, matching them
# to the column headings. Because a couple of values can contain whitespace, 
# these headings have special handling, and a 'fragment offset' is maintained.
# Specifically:
# - The 'Esid' column can contain '*       -' for a mmap'd source segment
# - The 'Description' column can contain many fragments
# AFAIK, none of the other columns ever contain whitespace.
# Some entries are on multiple lines. This is detected by the presence of
# whitespace at the beginning of the line. Only secondary lines countaining
# 'source' are considered significant (as they list mmap'd client segments).
# ---------------------------Example-input-lines-------------------------------
#   3854e5 *       - work mmap source              sm  74312     0    0   74312
#   3a54ed         c mmap maps 5 source(s)         sm      0     0    -       -
#                    source(s)=5bd56b, 5b956a, 58d567, 589566, 3a14ec
#     8002         0 work fork tree                 m  31360 28992    0   31360
#                    children=7f1e80, 0
# -----------------------------------------------------------------------------
# Leaves results in arrays:
# - client_seg_list    - list of client segments
# - client_sources     - list of client segments which have been mmap'd
# - client_segs        - client segment data (multi-dimensional)
# - work_seg_list      - list of work segments
# - work_segs          - work segment data (multi-dimensional)
#
state == "SEGMENT_LIST_ENTRIES" {
  if ( /^$/ ) {
    # Blank line - end of the list - calculate totals.
	if ( VERBOSE ) print "Line", line ":", "End of segment list"
	list_total()
	state = next_state
	next_state = "ERROR"
  }
  else if ( substr( $0, 1, 8 ) == "        " ) {
    # Another line from a multi-line entry
    if ( /source/ ) {
	  if ( VERBOSE ) print "Line", line ":", "Parsing mmap sources list"
	  sourcelist = substr( $0, index( $0, "=" )+1 )
	  split( sourcelist, sources, ", " )
	  for ( idx in sources ) {
	    if ( VERBOSE ) print "Adding source Vsid", sources[idx]
	    client_sources[sources[idx]] = sources[idx]
	  }
	} else {
	  if ( VERBOSE ) print "Line", line ":", "Ignoring multi-line"
	}
  }
  else {
	# A normal list entry
	if ( VERBOSE ) print "Line", line ":", "Parsing segment"
	frag_count = split( $0, value_fragments )
	frag_offset = 0
    for ( idx=1; idx <= head_count; idx++ ) {
	  value = value_fragments[idx + frag_offset]
	  if ( headings[idx] == "Description" ) {
	    if (frag_count + frag_offset < head_count) {
		  if ( VERBOSE ) print "No description (head_count=" head_count ", frag_count=" frag_count ", frag_offset=" frag_offset ")"
		  value = ""
		  frag_offset--
		} else {
		  frag_pos = (frag_count - frag_offset)
	      for (offset=1; offset <= (frag_pos - head_count); offset++ ) {
		    if ( VERBOSE ) print "Combining Description fragments"
            value = ((value " ") value_fragments[idx + ++frag_offset])
		  }
		}
	  }
	  if ( headings[idx] == "Esid" && value == "*" ) {
	    if ( VERBOSE ) print "Combining Esid fragments"
	    value = ((value "       ") value_fragments[idx + ++frag_offset])
	  }
	  values[headings[idx]] = value
	  if ( VERBOSE ) print idx ":", headings[idx], "=", value
	}
	if (values["Type"] == "clnt" && values["Esid"] == "-") {
	  # Normally we ignore client segments, but we need to keep them until we
	  # know whether they were mmap'd
	  client_seg_list[values["Vsid"]] = values["Vsid"]
	  for ( idx in headings ) {
		client_segs[values["Vsid"], headings[idx]] = values[headings[idx]]
	  }
	} else {
	  # We want to total up all other segments
      work_seg_list[values["Vsid"]] = values["Vsid"]
	  for ( idx in headings ) {
	    work_segs[values["Vsid"], headings[idx]] = values[headings[idx]]
	  }
	}
  }
}

# Calculates the totals from the arrays built while parsing a segment list.
# This is implemented as a function so that it can be called from END as well
# as during normal parsing (since the end of the SHARED list is also end of
# input).
# Leaves results in variables:
#     work_total_inuse, work_total_virtual, forktree_total_inuse,
#     forktree_total_virtual, client_total_inuse, client_total_virtual,
#     client_mmap_total_inuse, client_mmap_total_virtual
# Empties arrays: client_sources, client_seg_list, client_segs,
#     work_seg_list, work_segs
#
function list_total() {
  # separate any mmap'd client segments
  for ( source in client_sources ) {
    if ( source in work_seg_list ) {
	  if ( VERBOSE ) print "Vsid", source, "is in the work list (probably mmap'd work segment)"
	} else {
	  if ( client_segs[source, "Vsid"] != "" ) {
		client_mmap_seg_list[source] = source
		for ( idx in headings ) {
		  client_mmap_segs[source, headings[idx]] = client_segs[source, headings[idx]]
		}
		# we are accounting for this segment in client_mmap_seg_list, so delete
		# it from client_seg_list
		delete client_seg_list[source]
	  } else {
	    if ( VERBOSE ) print "Vsid", source, "not found in segment list - forward reference?"
      }
	}
	delete client_sources[source]
  }
  work_total_inuse = 0
  work_total_virtual = 0
  forktree_total_inuse = 0
  forktree_total_virtual = 0
  for ( idx in work_seg_list ) {
	if ( VERBOSE ) print "Segment", idx, "included - Type:", work_segs[idx, "Type"] ", Inuse:", work_segs[idx, "Inuse"] ", Virtual:", work_segs[idx, "Virtual"]
	if ( work_segs[idx, "Description"] == "fork tree" || work_segs[idx, "Description"] == "shared library" || work_segs[idx, "Description"] == "shared library text" || work_segs[idx, "Description"] == "kernel segment" ) {
	  # We want to record fork tree statistics separately, as they can contain
	  # any number of libraries (not all of which we are responsible for)
	  # Note: After upgrading from AIX 6.1 TL02 to TL06, 'fork tree' is now
	  # reported as either 'shared library' or 'shared library text'.
	  forktree_total_inuse += work_segs[idx, "Inuse"]
	  forktree_total_virtual += work_segs[idx, "Virtual"]
	} else {
	  work_total_inuse += work_segs[idx, "Inuse"]
	  work_total_virtual += work_segs[idx, "Virtual"]
	}
	delete work_seg_list[idx]
  }
  client_total_inuse = 0
  client_total_virtual = 0
  for ( idx in client_seg_list ) {
	if ( VERBOSE ) print "Client Segment", idx, "excluded - Inuse:", client_segs[idx, "Inuse"] ", Virtual:", client_segs[idx, "Virtual"]
	client_total_inuse += client_segs[idx, "Inuse"]
	client_total_virtual += client_segs[idx, "Virtual"]
	delete client_seg_list[idx]
  }
  client_mmap_total_inuse = 0
  client_mmap_total_virtual = 0
  for ( idx in client_mmap_seg_list ) {
	if ( VERBOSE ) print "Client Segment", idx, "excluded - Inuse:", client_segs[idx, "Inuse"] ", Virtual:", client_segs[idx, "Virtual"]
	client_mmap_total_inuse += client_mmap_segs[idx, "Inuse"]
	client_mmap_total_virtual += client_mmap_segs[idx, "Virtual"]
	delete client_mmap_seg_list[idx]
  }
  for ( idx in work_segs ) { delete work_segs[idx] }
  for ( idx in client_segs ) { delete client_segs[idx] }
  for ( idx in client_mmap_segs ) { delete client_mmap_segs[idx] }
}

# Records the Inuse and Virtual totals from the SYSTEM segment list.
# Leaves results in variables: system_work_inuse, system_work_virtual,
#     system_forktree_inuse, system_forktree_virtual,
#     system_client_inuse, system_client_virtual
#
state == "SYSTEM_LIST" {
  system_work_inuse = work_total_inuse
  system_work_virtual = work_total_virtual
  system_forktree_inuse = forktree_total_inuse
  system_forktree_virtual = forktree_total_virtual
  system_client_inuse = client_total_inuse
  system_client_virtual = client_total_virtual
  system_client_mmap_inuse = client_mmap_total_inuse
  system_client_mmap_virtual = client_mmap_total_virtual
  state = "SEARCH"
}

# Records the Inuse and Virtual totals from the EXCLUSIVE segment list.
# Leaves results in variables: exclusive_work_inuse, exclusive_work_virtual,
#     exclusive_forktree_inuse, exclusive_forktree_virtual,
#     exclusive_client_inuse, exclusive_client_virtual
#
state == "EXCLUSIVE_LIST" {
  exclusive_work_inuse = work_total_inuse
  exclusive_work_virtual = work_total_virtual
  exclusive_forktree_inuse = forktree_total_inuse
  exclusive_forktree_virtual = forktree_total_virtual
  exclusive_client_inuse = client_total_inuse
  exclusive_client_virtual = client_total_virtual
  exclusive_client_mmap_inuse = client_mmap_total_inuse
  exclusive_client_mmap_virtual = client_mmap_total_virtual
  state = "SEARCH"
}

# Matches the start of a procmap report for a Java process.
# ---------------------------Example-input-line--------------------------------
# 286952 : <PATH>/bin/java parm1 parm2 parm3
# -----------------------------------------------------------------------------
state == "PROCMAP_START" {
  if ( VERBOSE ) print "Found start of PROCMAP report"
  state = "PROCMAP"
  procmap_readexec = 0
  procmap_readwrite = 0
  next
}

# Matches lines from a procmap report
# ---------------------------Example-input-lines-------------------------------
# 100000000        122K  read/exec         java
# 110000cdb          1K  read/write        java
# 9fffffff0000000        51K  read/exec         /usr/ccs/bin/usla64
# 9fffffff000cfea         0K  read/write        /usr/ccs/bin/usla64
# -----------------------------------------------------------------------------
state == "PROCMAP" {
  procmap_region_size = $2
  sub(/K/, "", procmap_region_size)
  if ( /read\/exec/ ) {
  	procmap_readexec = procmap_readexec + procmap_region_size
  }
  if ( /read\/write/ ) {
  	procmap_readwrite = procmap_readwrite + procmap_region_size
  }
}

# Matches the end of a procmap report for a Java process.
# Note: We only include the read/exec (shared library text) portions of this
# report, as the 'read/write' (our private shared library data) portions are
# already counted in the work segments.
# ---------------------------Example-input-line--------------------------------
#    Total       19886K
# -----------------------------------------------------------------------------
state == "PROCMAP_TOTAL" {
  if ( VERBOSE ) print "Found end of PROCMAP report"
  procmap_grand_total = $2
  sub(/K/, "", procmap_grand_total)
  if ( VERBOSE ) print "Procmap total:", procmap_grand_total, "kb"
  if ( VERBOSE ) print "Total read/exec:", procmap_readexec, "kb"
  if ( VERBOSE ) print "Total read/write:", procmap_readwrite, "kb"
  state = "SEARCH"
  next
}

# Should never get here :)
#
state == "ERROR" {
  print "Error"
  exit
}

# End of input.
# Records the Inuse and Virtual totals from the SHARED segment list first,
# because the end of the shared list is also end of input. Then calculates the
# grand totals parsed from section summaries and section segment lists.
# A summary is printed on stdout.
#
END {
  list_total()
  if ( next_state == "SHARED_LIST" ) {
	shared_work_inuse = work_total_inuse
	shared_work_virtual = work_total_virtual
    shared_forktree_inuse = forktree_total_inuse
    shared_forktree_virtual = forktree_total_virtual
    shared_client_inuse = client_total_inuse
    shared_client_virtual = client_total_virtual
    shared_client_mmap_inuse = client_mmap_total_inuse
    shared_client_mmap_virtual = client_mmap_total_virtual
  } else {
    print "Error - at the end of input, I expected to be parsing the SHARED segment list"
  }
  
  format = "%26s  %-10s %-10s\n"
  
  print "AIX Java Footprint Parser Report"
  print "================================"
  print "Command:", command
  if ( pid_report ) print "PID:", pid

  
  print "\n----- Summaries (from svmon itself) -----"
  summary_total_inuse = system_summary_inuse + exclusive_summary_inuse + shared_summary_inuse
  summary_total_virtual = system_summary_virtual + exclusive_summary_virtual + shared_summary_virtual

  printf format, "", "Inuse:", "Virtual:"
  printf format, "SYSTEM", system_summary_inuse, system_summary_virtual
  printf format, "EXCLUSIVE", exclusive_summary_inuse, exclusive_summary_virtual
  printf format, "SHARED", shared_summary_inuse, shared_summary_virtual
  printf format, "(totals)", summary_total_inuse, summary_total_virtual
  printf format, "Command summary", command_summary_inuse, command_summary_virtual
  if ( procmap_grand_total ) {
    printf format, "Procmap read/exec", procmap_readexec, ""
    printf format, "Procmap read/write", procmap_readwrite, ""
    printf format, "Procmap total", procmap_grand_total, ""
  }
  
  print "\n----- Totals (from parser) by segment type / category -----"
  printf format, "", "Inuse:", "Virtual:"
  printf format, "SYSTEM (working)", system_work_inuse, system_work_virtual
  printf format, "SYSTEM (forktree)", system_forktree_inuse, system_forktree_virtual
  printf format, "SYSTEM (client)", system_client_inuse, system_client_virtual
  printf format, "SYSTEM (mmap clnt)", system_client_mmap_inuse, system_client_mmap_virtual
  printf format, "EXCLUSIVE (working)", exclusive_work_inuse, exclusive_work_virtual
  printf format, "EXCLUSIVE (forktree)", exclusive_forktree_inuse, exclusive_forktree_virtual
  printf format, "EXCLUSIVE (client)", exclusive_client_inuse, exclusive_client_virtual
  printf format, "EXCLUSIVE (mmap clnt)", exclusive_client_mmap_inuse, exclusive_client_mmap_virtual
  printf format, "SHARED (working)", shared_work_inuse, shared_work_virtual
  printf format, "SHARED (forktree)", shared_forktree_inuse, shared_forktree_virtual
  printf format, "SHARED (client)", shared_client_inuse, shared_client_virtual
  printf format, "SHARED (mmap clnt)", shared_client_mmap_inuse, shared_client_mmap_virtual
  
  print "\n----- Totals by segment type -----"
  work_total_inuse = system_work_inuse + exclusive_work_inuse + shared_work_inuse
  work_total_virtual = system_work_virtual + exclusive_work_virtual + shared_work_virtual
  forktree_total_inuse = system_forktree_inuse + exclusive_forktree_inuse + shared_forktree_inuse
  forktree_total_virtual = system_forktree_virtual + exclusive_forktree_virtual + shared_forktree_virtual
  client_total_inuse = system_client_inuse + exclusive_client_inuse + shared_client_inuse
  client_total_virtual = system_client_virtual + exclusive_client_virtual + shared_client_virtual
  client_mmap_total_inuse = system_client_mmap_inuse + exclusive_client_mmap_inuse + shared_client_mmap_inuse
  client_mmap_total_virtual = system_client_mmap_virtual + exclusive_client_mmap_virtual + shared_client_mmap_virtual
  
  printf format, "", "Inuse:", "Virtual:"
  printf format, "Work segments", work_total_inuse, work_total_virtual
  printf format, "Work (fork tree)", forktree_total_inuse, forktree_total_virtual
  printf format, "Client segments", client_total_inuse, client_total_virtual
  printf format, "Mmapd client segments", client_mmap_total_inuse, client_mmap_total_virtual
  
  print "\n----- Footprint figures (various interpretations) -----"
  format = "%45s  %-10s %-10s\n"
  working_set_inuse = work_total_inuse + client_mmap_total_inuse
  working_set_virtual = work_total_virtual + client_mmap_total_virtual
  working_impact_inuse = working_set_inuse + client_total_inuse
  working_impact_virtual = working_set_virtual + client_total_virtual
  if ( procmap_grand_total ) {
# 20100903 - switching back to include mmap'd client segments as the shared class cache in Java626
# is now an mmap'd client segment.
#    jtc_footprint_inuse = work_total_inuse + procmap_readexec
    jtc_footprint_inuse = working_set_inuse + procmap_readexec
  }
  
  printf format, "", "Inuse:", "Virtual:"
  printf format, "Working set (work + mmap clnt segs)", working_set_inuse, working_set_virtual
  printf format, "Working impact (work + all clnt segs)", working_impact_inuse, working_impact_virtual
  printf format, "Working set (plus forktrees)", working_set_inuse + forktree_total_inuse, working_set_virtual + forktree_total_virtual
  printf format, "Working impact (plus forktrees)", working_impact_inuse + forktree_total_inuse, working_impact_virtual + forktree_total_virtual
  if ( procmap_grand_total ) {
    printf format, "JTC footprint (Working set + Procmap read/exec)", jtc_footprint_inuse, ""
  }
}
