const argv = process.argv.slice(2)
console.log('checking ' + argv[0]);
var lineReader = require('readline').createInterface({
  input: require('fs').createReadStream(argv[0])
});

let vid;
let i = 0;

lineReader.on('line', function (line) {
	const [userid, venueid, ...res] = line.split(';');
	
	if (vid === venueid) {
		console.log(i);
	}

	vid = venueid;
	i++;
});

process.exit(0);