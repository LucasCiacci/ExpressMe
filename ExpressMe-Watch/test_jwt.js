const crypto = require('crypto');
const fs = require('fs');
const https = require('https');

const keyFile = fs.readFileSync('app/src/main/res/raw/chave_firebase.json', 'utf8');
const keyData = JSON.parse(keyFile);

const header = { alg: 'RS256', typ: 'JWT', kid: keyData.private_key_id };
const now = Math.floor(Date.now() / 1000);
const claim = {
  iss: keyData.client_email,
  scope: 'https://www.googleapis.com/auth/firebase.messaging',
  aud: 'https://oauth2.googleapis.com/token',
  exp: now + 3600,
  iat: now - 60
};

const encodeB64 = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64url');
const signatureInput = `${encodeB64(header)}.${encodeB64(claim)}`;

const sign = crypto.createSign('RSA-SHA256');
sign.update(signatureInput);
const signature = sign.sign(keyData.private_key).toString('base64url');

const jwt = `${signatureInput}.${signature}`;

const req = https.request('https://oauth2.googleapis.com/token', {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
}, (res) => {
  let data = '';
  res.on('data', d => data += d);
  res.on('end', () => console.log('Response:', data));
});
req.write(`grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`);
req.end();
