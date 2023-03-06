const path = require('path');

/** @type {import('ts-jest/dist/types').InitialOptionsTsJest} */
module.exports = {
  preset: 'ts-jest',
  setupFilesAfterEnv: ['jest-expect-message'],
  // testEnvironment: 'jsdom',
  testMatch: [
    '<rootDir>/src/**/*.spec.ts'
  ],
  coverageReporters: [
    'html',
    'cobertura'
  ],
  collectCoverageFrom: [
    'src/**/*.ts'
  ],
  // globals: {
  //   'ts-jest': {
  //     babel: true,
  //     tsconfig: 'test/tsconfig.json'
  //   }
  // },
  verbose: true
};
