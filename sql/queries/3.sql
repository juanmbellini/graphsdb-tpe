WITH RECURSIVE trajectory(
	userid,
	pos,
	start,
	src,
	dst,
	depth
) AS 
(
   SELECT
      t.userid,
      t.tpos,
      t.utctimestamp as start,
      t.venueid as src,
      t.venueid as dst,
      1
   FROM
      trajectories_ll t 
      INNER JOIN
         categories c 
         ON c.venueid = t.venueid 
      UNION ALL
      SELECT
         t.userid,
         t.tpos,
         tt.start as start,
         tt.src as src,
         t.venueid as dst,
         tt.depth + 1
      FROM
         trajectories_ll t,
         trajectory tt,
         categories c 
      WHERE
         tt.userid = t.userid 
         and t.tpos = tt.pos + 1 
         and c.venueid = t.venueid 
         and EXTRACT(DAY from t.utctimestamp) = EXTRACT(day from tt.start) 
)
SELECT
   userid,
   pos - depth + 1 as start_pos,
   depth as length,
   start::date,
   src as venue
FROM
   trajectory 
where
   src = dst 
   and depth > 1;
