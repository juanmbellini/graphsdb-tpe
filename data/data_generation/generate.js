const argv = process.argv.slice(2)
		, fs = require('fs')
		, moment = require('moment')
		, _ = require('lodash')
		, cliProgress = require('cli-progress');

if (argv.length !== 1) {
	console.error(`generate 1.0.0
Usage: node generate.js <file>

Config options:
	<file>: JSON file containing program configuration
	
File parameters: the following are defaults and format
{
	"users": 1000,
	"interval": ['05/10/2010 08:30:20', '06/10/2010 22:00:00'], // defaults to [now() - 1 year, now()]
	"visits": [0, 5], // per day
	"speed": 5.5,
	"output": \`config-<random>.csv\`
}`);
	return process.exit(1);
}

const configFile = (() => {
	try {
		return JSON.parse(fs.readFileSync(argv[0]).toString());
	} catch (err) {
		return null;
	}
})();

if (!configFile) {
	console.error(`generate 1.0.0
${argv[0]} is not a valid JSON file`);
	return process.exit(2);
}

const defaultConfig = {
	users: 1000,
	interval: [moment().subtract(1, 'year'), moment()],
	visits: [0, 5],
	speed: 5.5,
	output: `generate-output-${Date.now() % 100}.csv`
};

const config = Object.assign({}, defaultConfig, configFile);

if (!Number.isFinite(config.users) && isNaN(parseInt(config.users, 10))) {
	console.error(`generate 1.0.0
users could not be parsed into a number`);
	return process.exit(9);
}

if (!Array.isArray(config.interval) || config.interval.length !== 2) {
	console.error(`generate 1.0.0
interval must be an array of the form ['05/10/2010 08:30:20', '06/10/2010 22:00:00']`);
	return process.exit(3);
}

if (!moment.isMoment(config.interval[0])) {
	if (!moment(config.interval[0]).isValid()) {
		console.error(`generate 1.0.0
interval[0] could not be parsed into a valid date`);
		return process.exit(3);
	}
	if (!moment(config.interval[1]).isValid()) {
		console.error(`generate 1.0.0
interval[1] could not be parsed into a valid date`);
		return process.exit(3);
	}

	if (moment(config.interval[0]).isAfter(config.interval[1])) {
		console.error(`generate 1.0.0
interval[0] is after interval[1]`);
		return process.exit(4);
	}

	config.interval = config.interval.map(i => moment.utc(i));
}

if (!Array.isArray(config.visits) || config.visits.length !== 2) {
	console.error(`generate 1.0.0
visits must be an array of the form [min: Number, max: Number]`);
	return process.exit(5);
}

if (!Number.isFinite(config.visits[0]) && isNaN(parseInt(config.visits[0], 10))) {
	console.error(`generate 1.0.0
visits[0] could not be parsed into a number`);
	return process.exit(6);
}
if (!Number.isFinite(config.visits[1]) && isNaN(parseInt(config.visits[1], 10))) {
	console.error(`generate 1.0.0
visits[1] could not be parsed into a number`);
	return process.exit(7);
}

config.visits = config.visits.map(Number);

if (config.visits[0] > config.visits[1]) {
	console.error(`generate 1.0.0
visits[0] is greater than visits[1]`);
	return process.exit(8);
}

if (!parseFloat(config.speed)) {
	console.error(`generate 1.0.0
speed can\'t be parsed into a number`);
	return process.exit(8);
}

config.speed = parseFloat(config.speed);

console.log('Running with the following configuration');
console.log(JSON.stringify(config, null, 2));

function distance(lat1, lon1, lat2, lon2, unit) {
	var radlat1 = Math.PI * lat1/180
	var radlat2 = Math.PI * lat2/180
	var theta = lon1-lon2
	var radtheta = Math.PI * theta/180
	var dist = Math.sin(radlat1) * Math.sin(radlat2) + Math.cos(radlat1) * Math.cos(radlat2) * Math.cos(radtheta);
	if (dist > 1) {
		dist = 1;
	}
	dist = Math.acos(dist)
	dist = dist * 180/Math.PI
	dist = dist * 60 * 1.1515
	if (unit=="K") { dist = dist * 1.609344 }
	if (unit=="N") { dist = dist * 0.8684 }
	return dist
}

const SECONDS_PER_DAY = 1440 * 60;



const outputFile = fs.createWriteStream(config.output);
outputFile.write(['userid', 'venueid', 'utctimestamp', 'tpos'].join(';') + '\n');

const bar = new cliProgress.Bar({}, cliProgress.Presets.shades_classic);
bar.start(config.users, 0);

const venues = require('./venues.json');

_.times(config.users, userid => {
	let tpos = 0;
	_.times(config.interval[1].diff(config.interval[0], 'days'), day => {
		const visits = _.random(config.visits[0], config.visits[1]);
		const times = _.chain(visits).times(i => _.random(0, SECONDS_PER_DAY - 1)).uniq().sortBy(v => v).value();
		const initialVenue = venues[_.random(0, venues.length - 1)];
		const trajectory = new Array(times.length);
		trajectory[0] = initialVenue;

		times.slice(1).reduce((trajectory, timeOfVisit, i) => {
			const currentVenue = _.findLast(trajectory, Boolean);

			const deltaT = (timeOfVisit - times[i - 1]) / 3600;
			const startIndex = _.random(0, venues.length - 1);
			// const possibleVenues = _.filter(venues, v => currentVenue != v && v.venueid != currentVenue.venueid);
			const predicate = v => {
				if (currentVenue == v || v.venueid == currentVenue.venueid) return false;
				const d = distance(currentVenue.latitude, currentVenue.longitude, v.latitude, v.longitude, 'K');
				if (d > config.speed * deltaT) return false;
				return true;
			};
			const firstSliceVenue = _.find(venues, predicate, startIndex);
			const secondSliceVenue = firstSliceVenue || _.find(venues, predicate);
			const nextVenue = firstSliceVenue || secondSliceVenue;

			trajectory[i + 1] = nextVenue;
			return trajectory;
		}, trajectory);

		trajectory.filter(Boolean).forEach((venue, i) => {
			outputFile.write([
				userid,
				venue.venueid,
				moment(config.interval[0]).add(day, 'days').add(times[i], 'seconds').format('DD/MM/YYYY HH:mm:ss'),
				tpos++
			].join(';') + '\n');
		});
	});
	bar.update(userid);
});
bar.stop();

outputFile.end();