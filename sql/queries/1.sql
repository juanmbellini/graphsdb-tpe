WITH RECURSIVE trajectory(userid, pos, path, depth, cattype) AS 
(
   SELECT
      t.userid,
      t.tpos,
      t.tpos || '',
      1,
      c.cattype 
   FROM
      trajectories_ll t 
      INNER JOIN
         categories c 
         ON c.venueid = t.venueid 
      UNION ALL
      SELECT
         t.userid,
         t.tpos,
         tt.path || ',' || t.tpos,
         tt.depth + 1,
         tt.cattype || ',' || c.cattype 
      FROM
         trajectories_ll t,
         trajectory tt,
         categories c 
      WHERE
         tt.userid = t.userid 
         and t.tpos = tt.pos + 1 
         and tt.depth <= 3 
         and c.venueid = t.venueid 
)
SELECT
   userid,
   path 
FROM
   trajectory 
WHERE
   depth = 3 
   and cattype = 'Home,Station,Airport';
