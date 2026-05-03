/** @type {import('@stryker-mutator/api/core').PartialStrykerOptions} */
const config = {
    testRunner: "jest",
    jest: {
        configFile: "jest.config.js",
        config: {
            testMatch: [
                "**/src/helper/getHref.test.js",
                "**/src/utils/sheduleUtils.test.js"
            ],
            testPathIgnorePatterns: ["/node_modules/"],
            coverageThreshold: undefined
        }
    },
    mutator: {
        plugins: [],
        excludedMutations: []
    },
    mutate: [
        "src/helper/getHref.js",
        "src/utils/sheduleUtils.js"
    ],
    reporters: ["html", "clear-text", "progress"],
    htmlReporter: {
        fileName: "reports/mutation/mutation.html"
    },
    coverageAnalysis: "perTest"
};
export default config;
