const needle = require('needle');
require('dotenv').config()
const token = process.env.BEARER_TOKEN;

const endpointUrl = "https://api.twitter.com/2/tweets/search/recent";

async function getRequest() {

   
    const params = {
        'query': '#tests'
    }

    const res = await needle('get', endpointUrl, params, {
        headers: {
            "User-Agent": "v2RecentSearchJS",
            "authorization": `Bearer ${token}`
        }
    })

    if (res.body) {
        return res.body;
    } else {
        throw new Error('Unsuccessful request');
    }
}

(async () => {

    try {
        // Make request
        const response = await getRequest();
        console.log(response.data)
        // For extracting github_url, repo name and test command from tweet
        response.data.forEach((item)=>{
            console.log(item)
        })

    } catch (e) {
        console.log(e);
        process.exit(-1);
    }
    process.exit();
})();
