extensions [time]

to schedule-events
  let dt (time:create "2000-01-01 10:00:00")
  time:anchor-schedule dt 1 "hour"
  time:schedule-event turtles ([[] -> fd 1]) 10
  time:schedule-event (turtle 1) ([[] -> fd 1]) 5
  tick
end

to schedule-events-shuffled
  let dt (time:create "2000-01-01 10:00:00")
  time:anchor-schedule dt 1 "hour"
  time:schedule-event-shuffled turtles ([[] -> fd 1]) 10
  time:schedule-repeating-event turtles [[] -> fd 1]) 2.5 1.0
end

to go
  time:go
  time:go-until 100.0
  time:go-until (time:create "2004-01-04 10:00:00")
end

to checksize
  let value time:size-of-schedule
end

; Order of testing
  ;; size-of-scheduling
  ;; time:go
  ;; time:clear-schedule
  ;; time:schedule-event
  ;; time:anchor-schedule
