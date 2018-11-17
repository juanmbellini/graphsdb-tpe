const { Pool } = require('pg')
		, Cursor = require('pg-cursor')
		, async = require('async');

const pool = new Pool({
	host: '192.168.0.6',
	user: 'mgoffan',
	password: 'mgoffan',
	database: 'explaintest'
});

async function main() {

	const client = await pool.connect()
	const text = 'SELECT * FROM test order by utctimestamp asc';

	const cursor = client.query(new Cursor(text));

	let rows;

	let count = 0;

	async.during(
		cb => {
			cursor.read(100, (err, _rows) => {
				if (err) return cb(err);
				rows = _rows;
				count += rows.length;
				console.log(`read ${count} records`);
				cb(null, rows.length);
			});
		},
		cb => {
			async.eachSeries(rows, (row, next) => {
				pool.query('select count(*) from trajectories where userid=$1', [row.userid], (err, response) => {
					if (err) return next(err);
					const tpos = parseInt(response.rows[0].count, 10);
					pool.query('insert into trajectories values($1, $2, $3, $4)', [row.userid, row.venueid, row.utctimestamp, tpos], next);
				});
			}, cb);
		},
		err => {
			cursor.close(() => {
				client.release()
			})
		}
	);
};

main().catch(err => {
	console.error(err);
});